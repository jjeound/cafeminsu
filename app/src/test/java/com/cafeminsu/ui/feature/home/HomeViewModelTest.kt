package com.cafeminsu.ui.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.RecommendationRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
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
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("안녕하세요, 민수님", content.greeting)
            assertEquals("menu-1", content.recommendedMenu.id)
            assertEquals("민수 시그니처 라떼", content.recommendedMenu.name)
            assertEquals(5_500, content.recommendedMenu.price)
            assertEquals(6_000, content.recommendedMenu.originalPrice)
            assertEquals(2, content.availableCouponCount)
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
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("안녕하세요, 민수님", content.greeting)
            assertEquals(0, content.availableCouponCount)
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
        AppResult.Failure(DomainError.NotFound)

    override suspend fun refreshMenus(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeOrderRepository(
    initialOrders: AppResult<List<Order>>,
) : OrderRepository {
    private val orders = MutableStateFlow(initialOrders)

    override suspend fun createOrderFromCart(cart: com.cafeminsu.domain.model.Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Validation("cart"))

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> = orders
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
