package com.cafeminsu.ui.feature.cart

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OrderType
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.OrderRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cartWithItemsProducesContentWithSubtotal() = runTest {
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(
                initialCart = sampleCart(
                    items = listOf(sampleCartItem(unitPrice = 5_000, quantity = 2)),
                    validation = CartValidation.Valid,
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(10_000, content.subtotal)
            assertEquals(CartValidation.Valid, content.validation)
            assertEquals(listOf("민수 라떼"), content.items.map { it.name })
            assertEquals(OrderType.DineIn, content.orderType)
            assertEquals("", content.requestNote)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyCartProducesEmptyState() = runTest {
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(initialCart = emptyCart()),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()
            assertEquals("담은 메뉴가 없어요", empty.message)
            assertEquals(CartValidation.Invalid(listOf(CartInvalidReason.Empty)), empty.validation)
            assertEquals(OrderType.DineIn, empty.orderType)
            assertEquals("", empty.requestNote)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun orderTypeToggleAndRequestNoteUpdateState() = runTest {
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(
                initialCart = sampleCart(
                    items = listOf(sampleCartItem(unitPrice = 5_000, quantity = 2)),
                    validation = CartValidation.Valid,
                ),
            ),
        )

        viewModel.uiState.test {
            assertEquals(OrderType.DineIn, awaitContent().orderType)

            viewModel.onOrderTypeSelected(OrderType.Takeout)
            val takeout = awaitContent()
            assertEquals(OrderType.Takeout, takeout.orderType)
            assertEquals("", takeout.requestNote)

            viewModel.onRequestNoteChange("얼음 적게 부탁드려요")
            val withNote = awaitContent()
            assertEquals(OrderType.Takeout, withNote.orderType)
            assertEquals("얼음 적게 부탁드려요", withNote.requestNote)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun quantityChangeAndRemoveAreReflectedByRepositoryFlow() = runTest {
        val repository = FakeCartRepository(
            initialCart = sampleCart(
                items = listOf(sampleCartItem(unitPrice = 4_500, quantity = 1)),
                validation = CartValidation.Valid,
            ),
        )
        val viewModel = viewModel(cartRepository = repository)

        viewModel.uiState.test {
            assertEquals(4_500, awaitContent().subtotal)

            viewModel.onQuantityChange(cartItemId = "cart-item-1", quantity = 3)
            val updated = awaitContent()
            assertEquals(13_500, updated.subtotal)
            assertEquals(3, updated.items.single().quantity)
            assertEquals(UpdateQuantityRequest("cart-item-1", 3), repository.updateQuantityRequests.single())

            viewModel.onRemove(cartItemId = "cart-item-1")
            val empty = awaitEmpty()
            assertEquals(RemoveRequest("cart-item-1"), repository.removeRequests.single())
            assertEquals(CartValidation.Invalid(listOf(CartInvalidReason.Empty)), empty.validation)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkoutWithEmptyCartExposesInvalidReasonAndDoesNotCreateOrder() = runTest {
        val orderRepository = FakeOrderRepository()
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(initialCart = emptyCart()),
            orderRepository = orderRepository,
        )

        viewModel.uiState.test {
            awaitEmpty()

            viewModel.onCheckout()

            val blocked = awaitEmpty(checkoutInProgress = false)
            assertEquals(CartValidation.Invalid(listOf(CartInvalidReason.Empty)), blocked.validation)
            assertEquals(0, orderRepository.createOrderCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkoutSoldOutExposesInvalidReasonAndDoesNotCreateOrder() = runTest {
        val invalid = CartValidation.Invalid(listOf(CartInvalidReason.SoldOut("latte")))
        val orderRepository = FakeOrderRepository()
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(
                initialCart = sampleCart(
                    items = listOf(sampleCartItem(menuItemId = "latte", unitPrice = 5_000, quantity = 2)),
                    validation = invalid,
                ),
            ),
            orderRepository = orderRepository,
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.onCheckout()

            val blocked = awaitContent(checkoutInProgress = false)
            assertEquals(invalid, blocked.validation)
            assertEquals(0, orderRepository.createOrderCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun validCheckoutCreatesOrderAndEmitsNavigationEvent() = runTest {
        val orderRepository = FakeOrderRepository(orderResult = AppResult.Success(sampleOrder(id = "order-42")))
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(
                initialCart = sampleCart(
                    items = listOf(sampleCartItem(unitPrice = 5_000, quantity = 2)),
                    validation = CartValidation.Valid,
                ),
            ),
            orderRepository = orderRepository,
        )

        viewModel.uiState.test {
            awaitContent()
            viewModel.onOrderTypeSelected(OrderType.Takeout)
            viewModel.onRequestNoteChange("얼음 적게")

            viewModel.events.test {
                viewModel.onCheckout()

                val event = awaitItem()
                assertEquals(CartEvent.NavigateToPayment("order-42"), event)
                assertEquals(1, orderRepository.createOrderCalls)
                assertEquals(10_000, orderRepository.createdCarts.single().subtotal)
                assertEquals(OrderType.Takeout, orderRepository.createdCarts.single().orderType)
                assertEquals("얼음 적게", orderRepository.createdCarts.single().requestNote)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkoutWhileInProgressIgnoresDuplicateCalls() = runTest {
        val createGate = CompletableDeferred<Unit>()
        val orderRepository = FakeOrderRepository(createGate = createGate)
        val viewModel = viewModel(
            cartRepository = FakeCartRepository(
                initialCart = sampleCart(
                    items = listOf(sampleCartItem(unitPrice = 5_000, quantity = 2)),
                    validation = CartValidation.Valid,
                ),
            ),
            orderRepository = orderRepository,
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.onCheckout()
            assertTrue(awaitContent(checkoutInProgress = true).checkoutInProgress)

            viewModel.onCheckout()
            assertEquals(1, orderRepository.createOrderCalls)

            createGate.complete(Unit)
            assertFalse(awaitContent(checkoutInProgress = false).checkoutInProgress)
            assertEquals(1, orderRepository.createOrderCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        cartRepository: FakeCartRepository = FakeCartRepository(initialCart = emptyCart()),
        orderRepository: FakeOrderRepository = FakeOrderRepository(),
    ): CartViewModel =
        CartViewModel(
            cartRepository = cartRepository,
            orderRepository = orderRepository,
        )

    private suspend fun ReceiveTurbine<CartUiState>.awaitSettledState(): CartUiState {
        val state = awaitItem()
        return if (state == CartUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<CartUiState>.awaitContent(
        checkoutInProgress: Boolean? = null,
    ): CartUiState.Content {
        while (true) {
            val state = awaitSettledState()
            if (state is CartUiState.Content && checkoutInProgress.matches(state.checkoutInProgress)) {
                return state
            }
        }
    }

    private suspend fun ReceiveTurbine<CartUiState>.awaitEmpty(
        checkoutInProgress: Boolean? = null,
    ): CartUiState.Empty {
        while (true) {
            val state = awaitSettledState()
            if (state is CartUiState.Empty && checkoutInProgress.matches(state.checkoutInProgress)) {
                return state
            }
        }
    }

    private fun Boolean?.matches(actual: Boolean): Boolean =
        this == null || this == actual
}

private class FakeCartRepository(
    initialCart: Cart,
    private val observeResult: AppResult<Cart> = AppResult.Success(initialCart),
    private val validateResult: AppResult<CartValidation>? = null,
) : CartRepository {
    private val cart = MutableStateFlow(observeResult)
    val updateQuantityRequests = mutableListOf<UpdateQuantityRequest>()
    val removeRequests = mutableListOf<RemoveRequest>()

    override fun observeCart(): Flow<AppResult<Cart>> = cart

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> = AppResult.Failure(DomainError.Unknown)

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> {
        updateQuantityRequests += UpdateQuantityRequest(cartItemId, quantity)
        val current = currentCart() ?: return AppResult.Failure(DomainError.Unknown)
        val updatedItems = if (quantity <= 0) {
            current.items.filterNot { it.id == cartItemId }
        } else {
            current.items.map { item ->
                if (item.id == cartItemId) {
                    item.copy(quantity = quantity)
                } else {
                    item
                }
            }
        }
        val updated = current.copyWithItems(updatedItems)
        cart.value = AppResult.Success(updated)
        return AppResult.Success(updated)
    }

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> {
        removeRequests += RemoveRequest(cartItemId)
        val current = currentCart() ?: return AppResult.Failure(DomainError.Unknown)
        val updated = current.copyWithItems(current.items.filterNot { it.id == cartItemId })
        cart.value = AppResult.Success(updated)
        return AppResult.Success(updated)
    }

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        validateResult ?: AppResult.Success(currentCart()?.validation ?: CartValidation.Invalid(listOf(CartInvalidReason.Empty)))

    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)

    private fun currentCart(): Cart? =
        when (val result = cart.value) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> null
        }

    private fun Cart.copyWithItems(items: List<CartItem>): Cart {
        val subtotal = items.sumOf { item -> item.unitPrice * item.quantity }
        return copy(
            items = items,
            subtotal = subtotal,
            validation = if (items.isEmpty()) {
                CartValidation.Invalid(listOf(CartInvalidReason.Empty))
            } else {
                CartValidation.Valid
            },
        )
    }
}

private class FakeOrderRepository(
    private val orderResult: AppResult<Order> = AppResult.Success(sampleOrder()),
    private val createGate: CompletableDeferred<Unit>? = null,
) : OrderRepository {
    var createOrderCalls = 0
        private set
    val createdCarts = mutableListOf<Cart>()

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> {
        createOrderCalls += 1
        createdCarts += cart
        createGate?.await()
        return orderResult
    }

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        MutableStateFlow(orderResult)

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        MutableStateFlow(AppResult.Success(emptyList()))
}

private data class UpdateQuantityRequest(
    val cartItemId: String,
    val quantity: Int,
)

private data class RemoveRequest(
    val cartItemId: String,
)

private fun sampleCart(
    items: List<CartItem>,
    validation: CartValidation,
): Cart =
    Cart(
        items = items,
        subtotal = items.sumOf { item -> item.unitPrice * item.quantity },
        validation = validation,
    )

private fun emptyCart(): Cart =
    sampleCart(
        items = emptyList(),
        validation = CartValidation.Invalid(listOf(CartInvalidReason.Empty)),
    )

private fun sampleCartItem(
    id: String = "cart-item-1",
    menuItemId: String = "latte",
    name: String = "민수 라떼",
    unitPrice: Int,
    quantity: Int,
    selectedOptions: List<SelectedOption> = listOf(
        SelectedOption(
            groupId = "size",
            optionId = "size-large",
            name = "라지",
            extraPrice = 700,
        ),
    ),
): CartItem =
    CartItem(
        id = id,
        menuItemId = menuItemId,
        name = name,
        unitPrice = unitPrice,
        selectedOptions = selectedOptions,
        quantity = quantity,
    )

private fun sampleOrder(id: String = "order-1"): Order =
    Order(
        id = id,
        orderNumber = "M001",
        items = listOf(sampleCartItem(unitPrice = 5_000, quantity = 2)),
        totalAmount = 10_000,
        status = OrderStatus.PendingPayment,
        createdAtMillis = 1_800_000_000_000L,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
