package com.cafeminsu.ui.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.RecommendationRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.domain.repository.StoreRepository
import org.junit.Assert.assertNull
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repositoriesWithDataProduceHomeContentState() = runTest {
        val recentOrders = listOf(
            sampleOrder(
                id = "order-1",
                orderNumber = "A-1001",
                itemName = "아메리카노 ICE",
                menuItemId = "americano",
                totalAmount = 4_500,
                createdAtMillis = System.currentTimeMillis() - OneDayMillis,
            ),
            sampleOrder(
                id = "order-2",
                orderNumber = "A-1002",
                itemName = "헤이즐넛 라떼",
                menuItemId = "latte",
                totalAmount = 6_000,
                createdAtMillis = System.currentTimeMillis() - ThreeDaysMillis,
            ),
        )
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu(basePrice = 5_500)))),
            orderRepository = FakeOrderRepository(AppResult.Success(recentOrders)),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(sampleGifticons()),
            ),
            sessionRepository = FakeSessionRepository(
                AuthState.Authenticated(
                    UserProfile(
                        id = "user-1",
                        displayName = "민수",
                        phoneLast4 = "1234",
                    ),
                ),
            ),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(sampleStore(name = "민수 강남점")),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("안녕하세요, 민수님", content.greeting)
            assertEquals("menu-1", content.recommendedMenu.id)
            assertEquals("민수 시그니처 라떼", content.recommendedMenu.name)
            assertEquals(5_500, content.recommendedMenu.price)
            assertEquals("민수 강남점", content.recommendedMenu.storeName)
            assertEquals("아메리카노 ICE", content.recentOrders[0].menuName)
            assertEquals("어제", content.recentOrders[0].orderedAtLabel)
            assertEquals("헤이즐넛 라떼", content.recentOrders[1].menuName)
            assertEquals("3일 전", content.recentOrders[1].orderedAtLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Failure(DomainError.Network)),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Error)
            val error = state as HomeUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyRecentOrdersStillProduceContentState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("안녕하세요, 민수님", content.greeting)
            // 매장 미선택 시 추천 카드 매장명은 비어 있어야 한다.
            assertNull(content.recommendedMenu.storeName)
            assertTrue(content.recentOrders.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyMenusProduceEmptyState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(emptyList())),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Empty)
            val empty = state as HomeUiState.Empty
            assertEquals("안녕하세요, 민수님", empty.greeting)
            assertEquals("추천할 메뉴가 아직 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun serverRecommendationOverridesMenuDerivedRecommendation() = runTest {
        val serverRecommendation = sampleMenu(basePrice = 4_800).copy(
            id = "menu-99",
            name = "오늘의 추천 라떼",
        )
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(
                AppResult.Success(serverRecommendation),
            ),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("menu-99", content.recommendedMenu.id)
            assertEquals("오늘의 추천 라떼", content.recommendedMenu.name)
            assertEquals(4_800, content.recommendedMenu.price)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recommendationFailureFallsBackToMenuDerivedRecommendation() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(
                AppResult.Failure(DomainError.Network),
            ),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            // 추천 실패는 에러 화면이 아니라 메뉴 파생 추천으로 폴백한다.
            assertEquals("menu-1", content.recommendedMenu.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankStoreNameProducesNullStoreName() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            orderRepository = FakeOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(sampleStore(name = "   ")),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertNull(content.recommendedMenu.storeName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reorderCreatesOrderAndEmitsNavigationEvent() = runTest {
        val reorderMenu = sampleMenu(basePrice = 4_500).copy(id = "americano", name = "아메리카노")
        val createdOrder = sampleOrder(
            id = "order-99",
            orderNumber = "M099",
            itemName = "아메리카노",
            menuItemId = "americano",
            totalAmount = 4_500,
            createdAtMillis = System.currentTimeMillis(),
        )
        val orderRepository = FakeOrderRepository(
            initialOrders = AppResult.Success(emptyList()),
            createOrderResult = AppResult.Success(createdOrder),
        )
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(reorderMenu))),
            orderRepository = orderRepository,
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.events.test {
            viewModel.onReorder("americano")
            val event = awaitItem()
            assertTrue(event is HomeEvent.NavigateToPayment)
            assertEquals("order-99", (event as HomeEvent.NavigateToPayment).orderId)

            cancelAndIgnoreRemainingEvents()
        }

        // 단발 주문 카트는 해당 메뉴·기본 옵션·수량 1·Valid 여야 한다.
        val cart = orderRepository.lastCart
        assertEquals(1, cart?.items?.size)
        val item = cart?.items?.first()
        assertEquals("americano", item?.menuItemId)
        assertEquals(1, item?.quantity)
        assertEquals(4_500, item?.unitPrice)
        assertTrue(item?.selectedOptions?.isEmpty() == true)
        assertEquals(4_500, cart?.subtotal)
        assertEquals(CartValidation.Valid, cart?.validation)
    }

    @Test
    fun reorderOrderCreationFailureEmitsReorderFailedEvent() = runTest {
        val reorderMenu = sampleMenu(basePrice = 4_500).copy(id = "americano")
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(reorderMenu))),
            orderRepository = FakeOrderRepository(
                initialOrders = AppResult.Success(emptyList()),
                createOrderResult = AppResult.Failure(DomainError.Network),
            ),
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.events.test {
            viewModel.onReorder("americano")
            val event = awaitItem()
            assertTrue(event is HomeEvent.ReorderFailed)
            assertEquals(
                "네트워크 연결을 확인하고 다시 시도해 주세요",
                (event as HomeEvent.ReorderFailed).message,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reorderWithUnknownMenuEmitsReorderFailedEvent() = runTest {
        val orderRepository = FakeOrderRepository(
            initialOrders = AppResult.Success(emptyList()),
            createOrderResult = AppResult.Success(
                sampleOrder(
                    id = "order-1",
                    orderNumber = "M001",
                    itemName = "아메리카노",
                    menuItemId = "americano",
                    totalAmount = 4_500,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            ),
        )
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            orderRepository = orderRepository,
            rewardRepository = FakeRewardRepository(
                initialStampCard = AppResult.Success(sampleStampCard()),
                initialGifticons = AppResult.Success(emptyList()),
            ),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
            recommendationRepository = FakeRecommendationRepository(),
            storeRepository = FakeStoreRepository(null),
        )

        viewModel.events.test {
            viewModel.onReorder("ghost-menu")
            val event = awaitItem()
            assertTrue(event is HomeEvent.ReorderFailed)

            cancelAndIgnoreRemainingEvents()
        }
        // 메뉴 조회 실패 시 주문을 생성하지 않는다.
        assertNull(orderRepository.lastCart)
    }

    private fun sampleStore(name: String): Store =
        Store(
            id = "store-1",
            name = name,
            address = "서울시 강남구",
            phone = "02-000-0000",
            distanceMeters = 120,
            latitude = 37.0,
            longitude = 127.0,
            status = StoreStatus.Open,
            closingTimeLabel = null,
            amenities = emptyList(),
        )

    private fun sampleMenu(basePrice: Int = 5_500): MenuItem =
        MenuItem(
            id = "menu-1",
            categoryId = "coffee",
            name = "민수 시그니처 라떼",
            description = "고소한 헤이즐넛 시럽 + 따뜻한 우유",
            basePrice = basePrice,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        )

    private fun sampleStampCard(): StampCard =
        StampCard(
            userId = "user-1",
            currentCount = 4,
            goalCount = 10,
            history = emptyList(),
        )

    private fun sampleGifticons(): List<Gifticon> =
        listOf(
            sampleGifticon(id = "gifticon-1", status = GifticonStatus.Available),
            sampleGifticon(id = "gifticon-2", status = GifticonStatus.Available),
            sampleGifticon(id = "gifticon-3", status = GifticonStatus.Expired),
        )

    private fun sampleGifticon(
        id: String,
        status: GifticonStatus,
    ): Gifticon =
        Gifticon(
            id = id,
            title = "무료 음료 1잔 쿠폰",
            barcodeValue = "barcode-$id",
            qrValue = "qr-$id",
            expiresAtMillis = 1_830_297_600_000L,
            status = status,
        )

    private fun sampleOrder(
        id: String,
        orderNumber: String,
        itemName: String,
        menuItemId: String,
        totalAmount: Int,
        createdAtMillis: Long,
    ): Order =
        Order(
            id = id,
            orderNumber = orderNumber,
            items = listOf(
                CartItem(
                    id = "$id-item",
                    menuItemId = menuItemId,
                    name = itemName,
                    unitPrice = totalAmount,
                    selectedOptions = listOf(
                        SelectedOption(
                            groupId = "shot",
                            optionId = "extra-shot",
                            name = "샷 추가",
                            extraPrice = 500,
                        ),
                        SelectedOption(
                            groupId = "size",
                            optionId = "tall",
                            name = "톨",
                            extraPrice = 0,
                        ),
                    ),
                    quantity = 1,
                ),
            ),
            totalAmount = totalAmount,
            status = OrderStatus.Completed,
            createdAtMillis = createdAtMillis,
        )

    private suspend fun ReceiveTurbine<HomeUiState>.awaitSettledState(): HomeUiState {
        val state = awaitItem()
        return if (state == HomeUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private companion object {
        const val OneDayMillis = 24L * 60L * 60L * 1000L
        const val ThreeDaysMillis = 3L * OneDayMillis
    }
}

private class FakeMenuRepository(
    initialMenus: AppResult<List<MenuItem>>,
) : MenuRepository {
    private val menus = MutableStateFlow(initialMenus)

    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> = menus

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
        when (val current = menus.value) {
            is AppResult.Success ->
                current.data.firstOrNull { it.id == menuItemId }
                    ?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(DomainError.NotFound)

            is AppResult.Failure -> current
        }

    override suspend fun refreshMenus(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeOrderRepository(
    initialOrders: AppResult<List<Order>>,
    private val createOrderResult: AppResult<Order> = AppResult.Failure(DomainError.Validation("cart")),
) : OrderRepository {
    private val orders = MutableStateFlow(initialOrders)
    var lastCart: Cart? = null
        private set

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> {
        lastCart = cart
        return createOrderResult
    }

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> = orders

    override fun observeRecentOrders(): Flow<AppResult<List<Order>>> = orders
}

private class FakeRewardRepository(
    initialStampCard: AppResult<StampCard>,
    initialGifticons: AppResult<List<Gifticon>>,
) : RewardRepository {
    private val stampCard = MutableStateFlow(initialStampCard)
    private val gifticons = MutableStateFlow(initialGifticons)

    override fun observeStampCard(): Flow<AppResult<StampCard>> = stampCard

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        stampCard.value

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        gifticons

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)
}

private class FakeRecommendationRepository(
    initialRecommendation: AppResult<MenuItem?> = AppResult.Success(null),
) : RecommendationRepository {
    private val recommendation = MutableStateFlow(initialRecommendation)

    override fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>> = recommendation
}

private class FakeStoreRepository(
    selectedStore: Store?,
) : StoreRepository {
    private val selected = MutableStateFlow(selectedStore)

    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override suspend fun getStore(storeId: String): AppResult<Store> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun selectStore(storeId: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override fun observeSelectedStore(): Flow<Store?> = selected
}

private class FakeSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> = AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}

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
