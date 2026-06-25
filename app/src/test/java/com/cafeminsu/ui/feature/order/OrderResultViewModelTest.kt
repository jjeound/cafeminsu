package com.cafeminsu.ui.feature.order

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.StampEvent
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.ui.navigation.Routes
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OrderResultViewModelTest {
    @get:Rule
    val mainDispatcherRule = OrderResultMainDispatcherRule()

    @Test
    fun exposesSuccessSummaryFromOrderAndStampCard() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals("A-2543", content.summary.orderNumber)
            assertEquals("8,500원", content.summary.paidAmountLabel)
            assertEquals("스탬프 1개가 적립됐어요 (8/10)", content.summary.stampMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mapsMissingOrderToNonRetryableError() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeOrderResultOrderRepository(
                initialResult = AppResult.Failure(DomainError.NotFound),
            ),
        )

        viewModel.uiState.test {
            val errorState = awaitFailureState()

            assertEquals(
                OrderResultUiState.Failure(
                    message = "주문을 찾지 못했어요",
                    retryable = false,
                ),
                errorState,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        orderId: String = "order-1",
        orderRepository: FakeOrderResultOrderRepository = FakeOrderResultOrderRepository(
            initialResult = AppResult.Success(sampleOrder()),
        ),
        rewardRepository: FakeOrderResultRewardRepository = FakeOrderResultRewardRepository(),
    ): OrderResultViewModel =
        OrderResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.ORDER_OK_ORDER_ID to orderId)),
            orderRepository = orderRepository,
            rewardRepository = rewardRepository,
        )

    private suspend fun ReceiveTurbine<OrderResultUiState>.awaitContent(): OrderResultUiState.Content {
        while (true) {
            val state = awaitItem()
            if (state is OrderResultUiState.Content) {
                return state
            }
        }
    }

    private suspend fun ReceiveTurbine<OrderResultUiState>.awaitFailureState(): OrderResultUiState.Failure {
        while (true) {
            val state = awaitItem()
            if (state is OrderResultUiState.Failure) {
                return state
            }
        }
    }
}

private class FakeOrderResultOrderRepository(
    initialResult: AppResult<Order>,
) : OrderRepository {
    private val order = MutableStateFlow(initialResult)

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Unknown)

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        order

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override fun observeRecentOrders(): Flow<AppResult<List<Order>>> =
        MutableStateFlow(AppResult.Success(emptyList()))
}

private class FakeOrderResultRewardRepository(
    private val stampCard: AppResult<StampCard> = AppResult.Success(sampleStampCard()),
) : RewardRepository {
    override fun observeStampCard(): Flow<AppResult<StampCard>> =
        MutableStateFlow(stampCard)

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        stampCard

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)
}

private fun sampleOrder(): Order =
    Order(
        id = "order-1",
        orderNumber = "A-2543",
        items = listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "latte",
                name = "바닐라라떼",
                unitPrice = 8_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 8_500,
        status = OrderStatus.Paid,
        createdAtMillis = 1_800_000_000_000L,
    )

private fun sampleStampCard(): StampCard =
    StampCard(
        userId = "user-1",
        currentCount = 8,
        goalCount = 10,
        history = listOf(
            StampEvent(
                id = "stamp-1",
                orderId = "order-1",
                count = 1,
                createdAtMillis = 1_800_000_000_000L,
            ),
        ),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class OrderResultMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
