package com.cafeminsu.ui.feature.owner.orders

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.OwnerOrderRepository
import com.cafeminsu.domain.scheduling.CongestionCalculator
import com.cafeminsu.domain.scheduling.OrderScheduler
import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingBadge
import com.cafeminsu.domain.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
class OwnerOrdersViewModelTest {
    @get:Rule
    val mainDispatcherRule = OwnerOrdersMainDispatcherRule()

    @Test
    fun defaultStateCalculatesCountsAndShowsNewOrders() = runTest {
        val viewModel = ownerOrdersViewModel(
            FakeOwnerOrdersRepository(
                initialResult = AppResult.Success(sampleOrders()),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(OwnerOrdersFilter.New, content.selectedFilter)
            assertEquals(1, content.counts.newCount)
            assertEquals(1, content.counts.preparingCount)
            assertEquals(1, content.counts.readyCount)
            assertEquals(listOf("#1042"), content.orders.map { it.orderNumberLabel })
            assertEquals("신규", content.orders.single().statusLabel)
            assertEquals("접수하기", content.orders.single().actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingFilterShowsOnlyMatchingOrders() = runTest {
        val viewModel = ownerOrdersViewModel(
            FakeOwnerOrdersRepository(
                initialResult = AppResult.Success(sampleOrders()),
            ),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.selectFilter(OwnerOrdersFilter.Preparing)
            val preparing = awaitContent()

            assertEquals(OwnerOrdersFilter.Preparing, preparing.selectedFilter)
            assertEquals(listOf("#1043"), preparing.orders.map { it.orderNumberLabel })
            assertEquals("준비중", preparing.orders.single().statusLabel)
            assertEquals("준비완료", preparing.orders.single().actionLabel)

            viewModel.selectFilter(OwnerOrdersFilter.Ready)
            val ready = awaitContent()

            assertEquals(OwnerOrdersFilter.Ready, ready.selectedFilter)
            assertEquals(listOf("#1044"), ready.orders.map { it.orderNumberLabel })
            assertEquals("준비완료", ready.orders.single().statusLabel)
            assertEquals("픽업완료", ready.orders.single().actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun advanceStatusAfterRepositorySuccessUpdatesFilteredListAndCounts() = runTest {
        val repository = FakeOwnerOrdersRepository(
            initialResult = AppResult.Success(sampleOrders()),
        )
        val viewModel = ownerOrdersViewModel(repository)

        viewModel.uiState.test {
            assertEquals("#1042", awaitContent().orders.single().orderNumberLabel)

            viewModel.advanceStatus("order-1042")
            val newEmpty = awaitEmpty()

            assertEquals(OrderStatus.Preparing, repository.lastAdvanceTo)
            assertEquals(0, newEmpty.counts.newCount)
            assertEquals(2, newEmpty.counts.preparingCount)
            assertEquals(1, newEmpty.counts.readyCount)
            assertEquals("새 주문이 없어요", newEmpty.message)

            viewModel.selectFilter(OwnerOrdersFilter.Preparing)
            val preparing = awaitContent()

            // 우선순위 정렬: 더 오래 기다린 #1043(1분 먼저 접수)이 #1042 보다 앞선다(FIFO 아님).
            assertEquals(listOf("#1043", "#1042"), preparing.orders.map { it.orderNumberLabel })

            viewModel.advanceStatus("order-1042")
            val preparingAfterAdvance = awaitContent()

            assertEquals(OrderStatus.Ready, repository.lastAdvanceTo)
            assertEquals(0, preparingAfterAdvance.counts.newCount)
            assertEquals(1, preparingAfterAdvance.counts.preparingCount)
            assertEquals(2, preparingAfterAdvance.counts.readyCount)
            assertEquals(listOf("#1043"), preparingAfterAdvance.orders.map { it.orderNumberLabel })

            viewModel.selectFilter(OwnerOrdersFilter.Ready)
            val ready = awaitContent()

            // 우선순위 정렬: 더 오래 기다린 #1044(2분 먼저 접수)이 #1042 보다 앞선다(FIFO 아님).
            assertEquals(listOf("#1044", "#1042"), ready.orders.map { it.orderNumberLabel })

            viewModel.advanceStatus("order-1042")
            val readyAfterPickup = awaitContent()

            assertEquals(OrderStatus.Completed, repository.lastAdvanceTo)
            assertEquals(0, readyAfterPickup.counts.newCount)
            assertEquals(1, readyAfterPickup.counts.preparingCount)
            assertEquals(1, readyAfterPickup.counts.readyCount)
            assertEquals(listOf("#1044"), readyAfterPickup.orders.map { it.orderNumberLabel })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptySelectedFilterProducesEmptyStateWithCounts() = runTest {
        val viewModel = ownerOrdersViewModel(
            FakeOwnerOrdersRepository(
                initialResult = AppResult.Success(
                    listOf(sampleOrder(id = "order-1043", orderNumber = "1043", status = OrderStatus.Preparing)),
                ),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is OwnerOrdersUiState.Empty)
            val empty = state as OwnerOrdersUiState.Empty
            assertEquals(OwnerOrdersFilter.New, empty.selectedFilter)
            assertEquals(0, empty.counts.newCount)
            assertEquals(1, empty.counts.preparingCount)
            assertEquals(0, empty.counts.readyCount)
            assertEquals("새 주문이 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = ownerOrdersViewModel(
            FakeOwnerOrdersRepository(
                initialResult = AppResult.Failure(DomainError.Network),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is OwnerOrdersUiState.Error)
            val error = state as OwnerOrdersUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ordersAreSortedByPriorityNotFifoAndAgedOrderIsUrgentWithEta() = runTest {
        // 같은 신규(접수) 주문 2건: #1100 은 방금, #1099 는 700초 전 접수(기아 임박).
        // FIFO(최신순)면 [#1100, #1099] 이지만 우선순위 정렬은 더 오래 기다린 #1099 가 앞선다.
        val viewModel = ownerOrdersViewModel(
            FakeOwnerOrdersRepository(
                initialResult = AppResult.Success(
                    listOf(
                        sampleOrder(
                            id = "order-1100",
                            orderNumber = "1100",
                            status = OrderStatus.Accepted,
                            createdAtMillis = FixedNow - MinuteMillis,
                        ),
                        sampleOrder(
                            id = "order-1099",
                            orderNumber = "1099",
                            status = OrderStatus.Accepted,
                            createdAtMillis = FixedNow - AgedWaitingMillis,
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(listOf("#1099", "#1100"), content.orders.map { it.orderNumberLabel })
            val aged = content.orders.first()
            assertEquals(SchedulingBadge.Urgent, aged.priorityBadge)
            assertEquals("약 3분", aged.etaLabel)
            assertEquals(SchedulingBadge.Normal, content.orders.last().priorityBadge)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun ownerOrdersViewModel(
        repository: OwnerOrderRepository,
        clock: Clock = FakeClock(FixedNow),
    ): OwnerOrdersViewModel =
        OwnerOrdersViewModel(
            ownerOrderRepository = repository,
            orderScheduler = OrderScheduler(),
            congestionCalculator = CongestionCalculator(),
            prepTimeEstimator = RulePrepTimeEstimator(),
            clock = clock,
        )

    private suspend fun ReceiveTurbine<OwnerOrdersUiState>.awaitContent(): OwnerOrdersUiState.Content {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerOrdersUiState.Content -> return state
                OwnerOrdersUiState.Loading -> Unit
                is OwnerOrdersUiState.Empty -> error("Expected content but was $state")
                is OwnerOrdersUiState.Error -> error("Expected content but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerOrdersUiState>.awaitEmpty(): OwnerOrdersUiState.Empty {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerOrdersUiState.Empty -> return state
                OwnerOrdersUiState.Loading -> Unit
                is OwnerOrdersUiState.Content -> Unit
                is OwnerOrdersUiState.Error -> error("Expected empty but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerOrdersUiState>.awaitSettledState(): OwnerOrdersUiState {
        val state = awaitItem()
        return if (state == OwnerOrdersUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }
}

private class FakeOwnerOrdersRepository(
    initialResult: AppResult<List<Order>>,
) : OwnerOrderRepository {
    private val orders = MutableStateFlow(initialResult)
    var lastAdvanceTo: OrderStatus? = null
        private set

    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        orders.map { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    if (filter == null) {
                        result.data
                    } else {
                        result.data.filter { it.status == filter }
                    },
                )

                is AppResult.Failure -> result
            }
        }

    override suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> {
        lastAdvanceTo = to
        val currentOrders = (orders.value as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.Unknown)
        val order = currentOrders.firstOrNull { it.id == orderId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updatedOrder = order.copy(status = to)
        orders.value = AppResult.Success(
            currentOrders.map { currentOrder ->
                if (currentOrder.id == orderId) updatedOrder else currentOrder
            },
        )
        return AppResult.Success(updatedOrder)
    }
}

private fun sampleOrders(): List<Order> =
    listOf(
        sampleOrder(
            id = "order-1042",
            orderNumber = "1042",
            status = OrderStatus.Accepted,
            createdAtMillis = SampleCreatedAtMillis,
        ),
        sampleOrder(
            id = "order-1043",
            orderNumber = "1043",
            status = OrderStatus.Preparing,
            createdAtMillis = SampleCreatedAtMillis - MinuteMillis,
        ),
        sampleOrder(
            id = "order-1044",
            orderNumber = "1044",
            status = OrderStatus.Ready,
            createdAtMillis = SampleCreatedAtMillis - MinuteMillis * 2,
        ),
        sampleOrder(
            id = "order-1045",
            orderNumber = "1045",
            status = OrderStatus.Completed,
            createdAtMillis = SampleCreatedAtMillis - MinuteMillis * 3,
        ),
    )

private fun sampleOrder(
    id: String = "order-1042",
    orderNumber: String = "1042",
    status: OrderStatus,
    createdAtMillis: Long = SampleCreatedAtMillis,
): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        items = listOf(
            CartItem(
                id = "$id-item-1",
                menuItemId = "americano",
                name = "아메리카노(L) ICE",
                unitPrice = 4_500,
                selectedOptions = listOf(
                    SelectedOption(
                        groupId = "temperature",
                        optionId = "ice",
                        name = "ICE",
                        extraPrice = 0,
                    ),
                ),
                quantity = 1,
            ),
            CartItem(
                id = "$id-item-2",
                menuItemId = "vanilla-latte",
                name = "바닐라라떼(R) HOT",
                unitPrice = 5_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 10_000,
        status = status,
        createdAtMillis = createdAtMillis,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerOrdersMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeClock(private val now: Long) : Clock {
    override fun nowMillis(): Long = now
}

private const val SampleCreatedAtMillis = 1_750_311_240_000L
private const val MinuteMillis = 60L * 1000L
private const val FixedNow = SampleCreatedAtMillis + 5L * MinuteMillis
private const val AgedWaitingMillis = 700L * 1000L
