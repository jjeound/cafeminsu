package com.cafeminsu.ui.feature.my

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
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
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MyMainDispatcherRule()

    @Test
    fun authenticatedProfileStatsAndCouponsProduceContentState() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeMyOrderRepository(
                AppResult.Success(
                    sampleOrders(count = 12),
                ),
            ),
            rewardRepository = FakeMyRewardRepository(
                stampCard = AppResult.Success(sampleStampCard(currentCount = 7, goalCount = 10)),
                gifticons = AppResult.Success(sampleGifticons(availableCount = 3)),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("진지원", content.profile.displayName)
            assertEquals("진", content.profile.initial)
            assertEquals("GOLD", content.profile.tierLabel)
            assertEquals(12, content.stats.orderCount)
            assertEquals(7, content.stats.stampCount)
            assertEquals(10, content.stats.stampGoalCount)
            assertEquals(3, content.stats.couponCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contentIncludesQuickMenusAndSettingsList() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(
                listOf("주문내역", "선물하기", "쿠폰", "알림설정"),
                content.quickMenus.map { it.label },
            )
            assertEquals(
                listOf("이용 약관", "자주 묻는 질문", "고객센터", "버전 정보", "로그아웃"),
                content.settings.map { it.label },
            )
            assertEquals("1588-1234", content.settings.first { it.id == "support" }.trailingText)
            assertEquals("v1.0.0", content.settings.first { it.id == "version" }.trailingText)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun authenticatedUserWithEmptyStatsStillProducesContentState() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeMyOrderRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeMyRewardRepository(
                stampCard = AppResult.Success(sampleStampCard(currentCount = 0, goalCount = 10)),
                gifticons = AppResult.Success(emptyList()),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(0, content.stats.orderCount)
            assertEquals(0, content.stats.stampCount)
            assertEquals(0, content.stats.couponCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onLogoutClearsSessionProducesNeedsLoginStateAndNavigateLoginEvent() = runTest {
        val sessionRepository = FakeMySessionRepository(authenticatedUser())
        val viewModel = viewModel(sessionRepository = sessionRepository)

        viewModel.events.test {
            val eventTurbine = this

            viewModel.uiState.test {
                awaitContent()

                viewModel.onLogout()

                assertEquals(MyEvent.NavigateLogin, eventTurbine.awaitItem())
                val needsLogin = awaitNeedsLogin()
                assertTrue(sessionRepository.clearSessionCalled)
                assertEquals("로그인이 필요해요", needsLogin.message)
                assertEquals("다시 로그인하기", needsLogin.actionLabel)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun orderHistoryFailureProducesErrorState() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeMyOrderRepository(AppResult.Failure(DomainError.Network)),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestAuthStateProducesNeedsLoginState() = runTest {
        val viewModel = viewModel(
            sessionRepository = FakeMySessionRepository(AuthState.Guest),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun expiredAuthStateProducesNeedsLoginState() = runTest {
        val viewModel = viewModel(
            sessionRepository = FakeMySessionRepository(AuthState.Expired),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        sessionRepository: FakeMySessionRepository = FakeMySessionRepository(authenticatedUser()),
        orderRepository: FakeMyOrderRepository = FakeMyOrderRepository(
            AppResult.Success(listOf(sampleOrder())),
        ),
        rewardRepository: FakeMyRewardRepository = FakeMyRewardRepository(
            stampCard = AppResult.Success(sampleStampCard()),
            gifticons = AppResult.Success(sampleGifticons(availableCount = 1)),
        ),
    ): MyViewModel =
        MyViewModel(
            sessionRepository = sessionRepository,
            orderRepository = orderRepository,
            rewardRepository = rewardRepository,
        )

    private fun authenticatedUser(): AuthState =
        AuthState.Authenticated(
            UserProfile(
                id = "user-1",
                displayName = "진지원",
                phoneLast4 = "1234",
            ),
        )

    private fun sampleOrders(count: Int): List<Order> =
        List(count) { index ->
            sampleOrder(
                id = "order-$index",
                orderNumber = "M${index.toString().padStart(3, '0')}",
            )
        }

    private fun sampleOrder(
        id: String = "order-1",
        orderNumber: String = "M001",
        totalAmount: Int = 5_500,
        status: OrderStatus = OrderStatus.Paid,
        createdAtMillis: Long = 1_803_974_400_000L,
    ): Order =
        Order(
            id = id,
            orderNumber = orderNumber,
            items = listOf(
                CartItem(
                    id = "cart-1",
                    menuItemId = "menu-1",
                    name = "민수 라떼",
                    unitPrice = totalAmount,
                    selectedOptions = emptyList(),
                    quantity = 1,
                ),
            ),
            totalAmount = totalAmount,
            status = status,
            createdAtMillis = createdAtMillis,
        )

    private fun sampleStampCard(
        currentCount: Int = 4,
        goalCount: Int = 10,
    ): StampCard =
        StampCard(
            userId = "user-1",
            currentCount = currentCount,
            goalCount = goalCount,
            history = emptyList(),
        )

    private fun sampleGifticons(availableCount: Int): List<Gifticon> =
        List(availableCount) { index ->
            Gifticon(
                id = "gifticon-$index",
                title = "쿠폰 $index",
                barcodeValue = "barcode-$index",
                qrValue = "qr-$index",
                expiresAtMillis = 1_830_297_600_000L,
                status = GifticonStatus.Available,
            )
        } + Gifticon(
            id = "gifticon-expired",
            title = "지난 쿠폰",
            barcodeValue = "barcode-expired",
            qrValue = "qr-expired",
            expiresAtMillis = 1_827_619_200_000L,
            status = GifticonStatus.Expired,
        )

    private suspend fun ReceiveTurbine<MyUiState>.awaitSettledState(): MyUiState {
        val state = awaitItem()
        return if (state == MyUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<MyUiState>.awaitContent(): MyUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is MyUiState.Content)
        return state as MyUiState.Content
    }

    private suspend fun ReceiveTurbine<MyUiState>.awaitErrorState(): MyUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is MyUiState.Error)
        return state as MyUiState.Error
    }

    private suspend fun ReceiveTurbine<MyUiState>.awaitNeedsLogin(): MyUiState.NeedsLogin {
        val state = awaitSettledState()
        assertTrue(state is MyUiState.NeedsLogin)
        return state as MyUiState.NeedsLogin
    }
}

private class FakeMySessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)
    var clearSessionCalled: Boolean = false
        private set

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> {
        clearSessionCalled = true
        authState.value = AuthState.Guest
        return AppResult.Success(Unit)
    }
}

private class FakeMyOrderRepository(
    initialOrderHistory: AppResult<List<Order>>,
) : OrderRepository {
    private val orderHistory = MutableStateFlow(initialOrderHistory)

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Validation("cart"))

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> = orderHistory
}

private class FakeMyRewardRepository(
    stampCard: AppResult<StampCard>,
    gifticons: AppResult<List<Gifticon>>,
) : RewardRepository {
    private val stampCard = MutableStateFlow(stampCard)
    private val gifticons = MutableStateFlow(gifticons)

    override fun observeStampCard(): Flow<AppResult<StampCard>> = stampCard

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        stampCard.value

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> = gifticons

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
