package com.cafeminsu.ui.feature.history

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.repository.OrderRepository
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
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = HistoryMainDispatcherRule()

    @Test
    fun observesHistoryAndSeparatesActiveAndPastOrders() = runTest {
        val repository = FakeHistoryOrderRepository(
            AppResult.Success(
                listOf(
                    sampleOrder(id = "past-1", status = OrderStatus.Completed),
                    sampleOrder(id = "active-1", status = OrderStatus.Preparing),
                ),
            ),
        )
        val viewModel = viewModel(repository = repository)

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("active-1", content.activeOrder?.id)
            assertEquals(listOf("past-1"), content.pastOrders.map { it.id })
            assertEquals(HistoryStepState.Current, content.activeOrder?.steps?.first { it.label == "준비중" }?.state)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyHistoryProducesEmptyState() = runTest {
        val viewModel = viewModel(
            repository = FakeHistoryOrderRepository(AppResult.Success(emptyList())),
        )

        viewModel.uiState.test {
            val empty = awaitSettledState()
            assertEquals(
                HistoryUiState.Empty(
                    title = "아직 주문 내역이 없어요",
                    message = "첫 번째 한 잔을 주문해보세요",
                ),
                empty,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesRetryableError() = runTest {
        val viewModel = viewModel(
            repository = FakeHistoryOrderRepository(AppResult.Failure(DomainError.Network)),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reorderPastOrderEmitsMenuDetailNavigationEvent() = runTest {
        val viewModel = viewModel(
            repository = FakeHistoryOrderRepository(
                AppResult.Success(listOf(sampleOrder(id = "past-1", status = OrderStatus.Completed))),
            ),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.onReorder("past-1")
                assertEquals(HistoryEvent.NavigateMenuDetail("vanilla-latte"), awaitItem())
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        repository: FakeHistoryOrderRepository,
        nowMillis: () -> Long = { JuneFifthNoonMillis },
    ): HistoryViewModel =
        HistoryViewModel(
            orderRepository = repository,
            nowMillis = nowMillis,
        )

    private suspend fun ReceiveTurbine<HistoryUiState>.awaitSettledState(): HistoryUiState {
        val state = awaitItem()
        return if (state == HistoryUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<HistoryUiState>.awaitContent(): HistoryUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is HistoryUiState.Content)
        return state as HistoryUiState.Content
    }

    private suspend fun ReceiveTurbine<HistoryUiState>.awaitErrorState(): HistoryUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is HistoryUiState.Error)
        return state as HistoryUiState.Error
    }
}

private class FakeHistoryOrderRepository(
    initialOrders: AppResult<List<Order>>,
) : OrderRepository {
    private val orders = MutableStateFlow(initialOrders)

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Unknown)

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> = orders
}

private fun sampleOrder(
    id: String,
    status: OrderStatus,
): Order =
    Order(
        id = id,
        orderNumber = "A-2419",
        items = listOf(
            CartItem(
                id = "$id-item-1",
                menuItemId = "vanilla-latte",
                name = "바닐라라떼",
                unitPrice = 5_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
            CartItem(
                id = "$id-item-2",
                menuItemId = "americano",
                name = "아메리카노",
                unitPrice = 4_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 10_000,
        status = status,
        createdAtMillis = JuneFifthMillis,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private const val JuneFifthMillis = 1_780_617_600_000L
private const val JuneFifthNoonMillis = 1_780_628_400_000L
