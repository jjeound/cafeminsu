package com.cafeminsu.ui.feature.owner.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.OwnerOrderRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = OwnerHomeMainDispatcherRule()

    @Test
    fun ordersAndStoreOpenProduceDashboardContentState() = runTest {
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = FakeOwnerOrderRepository(
                initialResult = AppResult.Success(
                    listOf(
                        sampleOrder(
                            id = "order-1042",
                            orderNumber = "1042",
                            itemName = "아메리카노(L) ICE",
                            totalAmount = 9_300,
                            status = OrderStatus.Accepted,
                        ),
                        sampleOrder(
                            id = "order-1041",
                            orderNumber = "1041",
                            itemName = "카페라떼(R) HOT",
                            totalAmount = 11_000,
                            status = OrderStatus.Preparing,
                        ),
                        sampleOrder(
                            id = "order-1040",
                            orderNumber = "1040",
                            itemName = "바닐라라떼(R) ICE",
                            totalAmount = 5_500,
                            status = OrderStatus.Completed,
                        ),
                    ),
                ),
            ),
            ownerAuthProvider = FakeOwnerAuthProvider(),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals("강남점", content.storeName)
            assertTrue(content.isStoreOpen)
            assertEquals(25_800, content.stats.totalSales)
            assertEquals(3, content.stats.orderCount)
            assertEquals(1, content.stats.newWaitingCount)
            assertEquals(2, content.pendingOrders.size)
            assertEquals("#1042", content.pendingOrders[0].orderNumberLabel)
            assertEquals("신규", content.pendingOrders[0].statusLabel)
            assertEquals("접수하기", content.pendingOrders[0].actionLabel)
            assertEquals("준비중", content.pendingOrders[1].statusLabel)
            assertEquals("준비완료", content.pendingOrders[1].actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun advanceStatusAfterRepositorySuccessUpdatesListAndCounts() = runTest {
        val repository = FakeOwnerOrderRepository(
            initialResult = AppResult.Success(
                listOf(
                    sampleOrder(
                        id = "order-1042",
                        orderNumber = "1042",
                        itemName = "아메리카노(L) ICE",
                        totalAmount = 9_300,
                        status = OrderStatus.Accepted,
                    ),
                ),
            ),
        )
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = repository,
            ownerAuthProvider = FakeOwnerAuthProvider(),
        )

        viewModel.uiState.test {
            assertEquals(1, awaitContent().stats.newWaitingCount)

            viewModel.advanceStatus("order-1042")

            val updated = awaitContent()
            assertEquals(OrderStatus.Preparing, repository.lastAdvanceTo)
            assertEquals(0, updated.stats.newWaitingCount)
            assertEquals("준비중", updated.pendingOrders.single().statusLabel)
            assertEquals("준비완료", updated.pendingOrders.single().actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setStoreOpenTogglesAfterProviderSuccess() = runTest {
        val ownerAuthProvider = FakeOwnerAuthProvider()
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = FakeOwnerOrderRepository(
                initialResult = AppResult.Success(listOf(sampleOrder(status = OrderStatus.Accepted))),
            ),
            ownerAuthProvider = ownerAuthProvider,
        )

        viewModel.uiState.test {
            assertTrue(awaitContent().isStoreOpen)

            viewModel.setStoreOpen(open = false)

            val closed = awaitContent()
            assertEquals(false, ownerAuthProvider.lastRequestedOpen)
            assertFalse(closed.isStoreOpen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storesAreExposedAndSelectStoreSwitchesActiveStore() = runTest {
        val ownerAuthProvider = FakeOwnerAuthProvider(
            stores = listOf(
                OwnerStore(id = "store-gangnam", name = "강남점"),
                OwnerStore(id = "store-hongdae", name = "홍대점"),
            ),
        )
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = FakeOwnerOrderRepository(
                initialResult = AppResult.Success(listOf(sampleOrder(status = OrderStatus.Accepted))),
            ),
            ownerAuthProvider = ownerAuthProvider,
        )

        viewModel.uiState.test {
            val initial = awaitContent()
            assertEquals("강남점", initial.storeName)
            assertEquals(listOf("강남점", "홍대점"), initial.stores.map { it.name })
            assertEquals("강남점", initial.stores.single { it.isSelected }.name)

            viewModel.selectStore("store-hongdae")

            val switched = awaitContent()
            assertEquals("store-hongdae", ownerAuthProvider.lastSelectedStoreId)
            assertEquals("홍대점", switched.storeName)
            assertEquals("홍대점", switched.stores.single { it.isSelected }.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = FakeOwnerOrderRepository(
                initialResult = AppResult.Failure(DomainError.Network),
            ),
            ownerAuthProvider = FakeOwnerAuthProvider(),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is OwnerHomeUiState.Error)
            val error = state as OwnerHomeUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyOrdersProduceEmptyStateWithZeroStats() = runTest {
        val viewModel = OwnerHomeViewModel(
            ownerOrderRepository = FakeOwnerOrderRepository(
                initialResult = AppResult.Success(emptyList()),
            ),
            ownerAuthProvider = FakeOwnerAuthProvider(),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is OwnerHomeUiState.Empty)
            val empty = state as OwnerHomeUiState.Empty
            assertEquals("강남점", empty.storeName)
            assertEquals(0, empty.stats.totalSales)
            assertEquals(0, empty.stats.orderCount)
            assertEquals(0, empty.stats.newWaitingCount)
            assertEquals("처리할 주문이 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<OwnerHomeUiState>.awaitContent(): OwnerHomeUiState.Content {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerHomeUiState.Content -> return state
                is OwnerHomeUiState.Loading -> Unit
                is OwnerHomeUiState.Empty -> error("Expected content but was $state")
                is OwnerHomeUiState.Error -> error("Expected content but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerHomeUiState>.awaitSettledState(): OwnerHomeUiState {
        val state = awaitItem()
        return if (state == OwnerHomeUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }
}

private class FakeOwnerOrderRepository(
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

private class FakeOwnerAuthProvider(
    private val stores: List<OwnerStore> = listOf(OwnerStore(id = "store-gangnam", name = "강남점")),
    private var ownerProfile: OwnerProfile = ownerProfile(),
) : OwnerAuthProvider {
    var lastRequestedOpen: Boolean? = null
        private set
    var lastSelectedStoreId: String? = null
        private set

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> =
        AppResult.Success(ownerProfile)

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> {
        lastRequestedOpen = open
        ownerProfile = ownerProfile.copy(isStoreOpen = open)
        return AppResult.Success(ownerProfile)
    }

    override suspend fun getStores(): AppResult<List<OwnerStore>> =
        AppResult.Success(stores)

    override suspend fun selectStore(storeId: String): AppResult<OwnerProfile> {
        lastSelectedStoreId = storeId
        val store = stores.firstOrNull { it.id == storeId }
            ?: return AppResult.Failure(DomainError.NotFound)
        ownerProfile = ownerProfile.copy(storeId = store.id, storeName = store.name)
        return AppResult.Success(ownerProfile)
    }
}

private fun ownerProfile(isStoreOpen: Boolean = true): OwnerProfile =
    OwnerProfile(
        id = "owner-demo",
        storeId = "store-gangnam",
        storeName = "강남점",
        loginId = "owner",
        isStoreOpen = isStoreOpen,
    )

private fun sampleOrder(
    id: String = "order-1042",
    orderNumber: String = "1042",
    itemName: String = "아메리카노(L) ICE",
    totalAmount: Int = 9_300,
    status: OrderStatus,
): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        items = listOf(
            CartItem(
                id = "$id-item-1",
                menuItemId = "americano",
                name = itemName,
                unitPrice = totalAmount,
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
        ),
        totalAmount = totalAmount,
        status = status,
        createdAtMillis = SampleCreatedAtMillis,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerHomeMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private const val SampleCreatedAtMillis = 1_750_311_240_000L
