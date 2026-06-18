package com.cafeminsu.ui.feature.my

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
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
    fun authenticatedUserWithOrderHistoryProducesContentState() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeMyOrderRepository(
                AppResult.Success(
                    listOf(sampleOrder(orderNumber = "M042")),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("민수", content.profile.displayName)
            assertEquals("1234", content.profile.phoneLast4)
            assertEquals("M042", content.recentOrders.single().orderNumber)
            assertEquals("결제 완료", content.recentOrders.single().statusLabel)
            assertEquals("로그아웃", content.settings.single().label)
            assertEquals("앱 버전 1.0", content.appMeta)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun authenticatedUserWithEmptyOrderHistoryProducesEmptyState() = runTest {
        val viewModel = viewModel(
            orderRepository = FakeMyOrderRepository(AppResult.Success(emptyList())),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()
            assertEquals("주문 내역이 없어요", empty.message)
            assertEquals("메뉴 보러가기", empty.actionLabel)
            assertEquals("민수", empty.profile.displayName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onLogoutClearsSessionAndProducesNeedsLoginState() = runTest {
        val sessionRepository = FakeMySessionRepository(authenticatedUser())
        val viewModel = viewModel(sessionRepository = sessionRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.onLogout()

            val needsLogin = awaitNeedsLogin()
            assertTrue(sessionRepository.clearSessionCalled)
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

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
    ): MyViewModel =
        MyViewModel(
            sessionRepository = sessionRepository,
            orderRepository = orderRepository,
        )

    private fun authenticatedUser(): AuthState =
        AuthState.Authenticated(
            UserProfile(
                id = "user-1",
                displayName = "민수",
                phoneLast4 = "1234",
            ),
        )

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

    private suspend fun ReceiveTurbine<MyUiState>.awaitEmpty(): MyUiState.Empty {
        val state = awaitSettledState()
        assertTrue(state is MyUiState.Empty)
        return state as MyUiState.Empty
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
