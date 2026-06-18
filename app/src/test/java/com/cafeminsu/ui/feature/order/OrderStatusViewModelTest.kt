package com.cafeminsu.ui.feature.order

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.repository.OrderRepository
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatusViewModelTest {
    @get:Rule
    val mainDispatcherRule = OrderStatusMainDispatcherRule()

    @Test
    fun observesOrderForSavedOrderIdAndReflectsStatusChanges() = runTest {
        val repository = FakeOrderStatusRepository(
            initialResult = AppResult.Success(sampleOrder(id = "order-42", status = OrderStatus.Paid)),
        )
        val viewModel = viewModel(orderId = "order-42", repository = repository)

        viewModel.uiState.test {
            val paid = awaitContent()
            assertEquals(listOf("order-42"), repository.observedOrderIds)
            assertEquals("order-42", paid.orderId)
            assertEquals("M042", paid.orderNumber)
            assertEquals(OrderStatus.Paid, paid.status)
            assertEquals("주문이 들어갔어요", paid.headerTitle)
            assertEquals(12_000, paid.totalAmount)
            assertEquals(listOf("민수 라떼"), paid.items.map { it.name })
            assertEquals(OrderStatusStepState.Completed, paid.steps.stepFor(OrderStatus.PendingPayment).state)
            assertEquals(OrderStatusStepState.Current, paid.steps.stepFor(OrderStatus.Paid).state)

            repository.emit(
                AppResult.Success(sampleOrder(id = "order-42", status = OrderStatus.Preparing)),
            )

            val preparing = awaitContent()
            assertEquals(OrderStatus.Preparing, preparing.status)
            assertEquals(OrderStatusStepState.Completed, preparing.steps.stepFor(OrderStatus.Accepted).state)
            assertEquals(OrderStatusStepState.Current, preparing.steps.stepFor(OrderStatus.Preparing).state)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failureFromRepositoryProducesRetryableError() = runTest {
        val viewModel = viewModel(
            repository = FakeOrderStatusRepository(
                initialResult = AppResult.Failure(DomainError.NotFound),
            ),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()
            assertEquals("주문을 찾지 못했어요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun orderStatusLabelsAndStepProgressAreMappedForDisplay() {
        assertEquals("결제 확인 중", OrderStatus.PendingPayment.orderStatusLabel())
        assertEquals("결제 완료", OrderStatus.Paid.orderStatusLabel())
        assertEquals("주문 접수", OrderStatus.Accepted.orderStatusLabel())
        assertEquals("준비 중", OrderStatus.Preparing.orderStatusLabel())
        assertEquals("픽업 준비 완료", OrderStatus.Ready.orderStatusLabel())
        assertEquals("픽업 완료", OrderStatus.Completed.orderStatusLabel())
        assertEquals("주문 취소", OrderStatus.Cancelled.orderStatusLabel())
        assertEquals("주문 실패", OrderStatus.Failed.orderStatusLabel())

        val readySteps = orderStatusSteps(OrderStatus.Ready)
        assertEquals(OrderStatusStepState.Completed, readySteps.stepFor(OrderStatus.Preparing).state)
        assertEquals(OrderStatusStepState.Current, readySteps.stepFor(OrderStatus.Ready).state)
        assertEquals(OrderStatusStepState.Upcoming, readySteps.stepFor(OrderStatus.Completed).state)

        val failedSteps = orderStatusSteps(OrderStatus.Failed)
        assertEquals(listOf(OrderStatus.Failed), failedSteps.map { it.status })
        assertEquals(OrderStatusStepState.Current, failedSteps.single().state)
    }

    private fun viewModel(
        orderId: String = "order-1",
        repository: FakeOrderStatusRepository,
    ): OrderStatusViewModel =
        OrderStatusViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(Routes.ORDER_STATUS_ORDER_ID to orderId),
            ),
            orderRepository = repository,
        )

    private suspend fun ReceiveTurbine<OrderStatusUiState>.awaitContent(): OrderStatusUiState.Content {
        while (true) {
            val state = awaitItem()
            if (state is OrderStatusUiState.Content) {
                return state
            }
        }
    }

    private suspend fun ReceiveTurbine<OrderStatusUiState>.awaitErrorState(): OrderStatusUiState.Error {
        while (true) {
            val state = awaitItem()
            if (state is OrderStatusUiState.Error) {
                return state
            }
        }
    }
}

private class FakeOrderStatusRepository(
    initialResult: AppResult<Order>,
) : OrderRepository {
    private val order = MutableStateFlow(initialResult)
    val observedOrderIds = mutableListOf<String>()

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Unknown)

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> {
        observedOrderIds += orderId
        return order
    }

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    fun emit(result: AppResult<Order>) {
        order.value = result
    }
}

private fun List<OrderStatusStepUiModel>.stepFor(status: OrderStatus): OrderStatusStepUiModel =
    first { it.status == status }

private fun sampleOrder(
    id: String = "order-1",
    status: OrderStatus = OrderStatus.Paid,
): Order =
    Order(
        id = id,
        orderNumber = "M042",
        items = listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "latte",
                name = "민수 라떼",
                unitPrice = 6_000,
                selectedOptions = emptyList(),
                quantity = 2,
            ),
        ),
        totalAmount = 12_000,
        status = status,
        createdAtMillis = 1_800_000_000_000L,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatusMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
