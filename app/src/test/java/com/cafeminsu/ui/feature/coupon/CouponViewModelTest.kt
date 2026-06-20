package com.cafeminsu.ui.feature.coupon

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Coupon
import com.cafeminsu.domain.model.CouponStatus
import com.cafeminsu.domain.model.CouponType
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.CouponRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CouponViewModelTest {
    @get:Rule
    val mainDispatcherRule = CouponMainDispatcherRule()

    @Test
    fun authenticatedStateMapsStampProgressAndCouponList() = runTest {
        val nowMillis = 1_803_974_400_000L
        val viewModel = viewModel(
            rewardRepository = FakeCouponRewardRepository(
                AppResult.Success(sampleStampCard(currentCount = 7, goalCount = 10)),
            ),
            couponRepository = FakeCouponRepository(
                AppResult.Success(
                    listOf(
                        sampleCoupon(id = "coupon-free", title = "무료 음료 1잔 쿠폰"),
                        sampleCoupon(
                            id = "coupon-amount",
                            title = "₩10,000",
                            type = CouponType.Amount,
                            amount = 10_000,
                        ),
                    ),
                ),
            ),
            nowMillis = { nowMillis },
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("강남점", content.stamp.storeName)
            assertEquals("7 / 10", content.stamp.countLabel)
            assertEquals(3, content.stamp.remainingCount)
            assertEquals(10, content.stamp.slots.size)
            assertEquals(7, content.stamp.slots.count { it.filled })
            assertEquals("8", content.stamp.slots[7].label)
            assertEquals("스탬프 3개만 더 모으면 무료 음료 쿠폰!", content.stamp.guideMessage)
            assertEquals(2, content.coupons.size)
            assertEquals("무료 음료 1잔 쿠폰", content.coupons.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun expiringUsedAndExpiredCouponsExposeDisplayStatus() = runTest {
        val nowMillis = 1_803_974_400_000L
        val viewModel = viewModel(
            couponRepository = FakeCouponRepository(
                AppResult.Success(
                    listOf(
                        sampleCoupon(
                            id = "soon",
                            expiresAtMillis = nowMillis + OneDayMillis,
                        ),
                        sampleCoupon(
                            id = "used",
                            status = CouponStatus.Used,
                        ),
                        sampleCoupon(
                            id = "expired",
                            status = CouponStatus.Expired,
                        ),
                    ),
                ),
            ),
            nowMillis = { nowMillis },
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertTrue(content.coupons.first { it.id == "soon" }.expiringSoon)
            assertFalse(content.coupons.first { it.id == "used" }.available)
            assertTrue(content.coupons.first { it.id == "used" }.dimmed)
            assertTrue(content.coupons.first { it.id == "expired" }.dimmed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesRetryableErrorState() = runTest {
        val viewModel = viewModel(
            couponRepository = FakeCouponRepository(AppResult.Failure(DomainError.Network)),
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
            sessionRepository = FakeCouponSessionRepository(AuthState.Guest),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        rewardRepository: FakeCouponRewardRepository = FakeCouponRewardRepository(
            AppResult.Success(sampleStampCard()),
        ),
        couponRepository: FakeCouponRepository = FakeCouponRepository(
            AppResult.Success(listOf(sampleCoupon())),
        ),
        sessionRepository: FakeCouponSessionRepository = FakeCouponSessionRepository(authenticatedUser()),
        nowMillis: () -> Long = { System.currentTimeMillis() },
    ): CouponViewModel =
        CouponViewModel(
            rewardRepository = rewardRepository,
            couponRepository = couponRepository,
            sessionRepository = sessionRepository,
            nowMillis = nowMillis,
        )

    private suspend fun ReceiveTurbine<CouponUiState>.awaitSettledState(): CouponUiState {
        val state = awaitItem()
        return if (state == CouponUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<CouponUiState>.awaitContent(): CouponUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is CouponUiState.Content)
        return state as CouponUiState.Content
    }

    private suspend fun ReceiveTurbine<CouponUiState>.awaitErrorState(): CouponUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is CouponUiState.Error)
        return state as CouponUiState.Error
    }

    private suspend fun ReceiveTurbine<CouponUiState>.awaitNeedsLogin(): CouponUiState.NeedsLogin {
        val state = awaitSettledState()
        assertTrue(state is CouponUiState.NeedsLogin)
        return state as CouponUiState.NeedsLogin
    }

    private companion object {
        const val OneDayMillis = 24L * 60L * 60L * 1000L
    }
}

private class FakeCouponRewardRepository(
    initialStampCard: AppResult<StampCard>,
) : RewardRepository {
    private val stampCard = MutableStateFlow(initialStampCard)

    override fun observeStampCard(): Flow<AppResult<StampCard>> = stampCard

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        stampCard.value

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)
}

private class FakeCouponRepository(
    initialCoupons: AppResult<List<Coupon>>,
) : CouponRepository {
    private val coupons = MutableStateFlow(initialCoupons)

    override fun observeCoupons(): Flow<AppResult<List<Coupon>>> = coupons

    override suspend fun useCoupon(id: String): AppResult<Coupon> =
        AppResult.Failure(DomainError.NotFound)
}

private class FakeCouponSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun sampleStampCard(
    currentCount: Int = 7,
    goalCount: Int = 10,
): StampCard =
    StampCard(
        userId = "user-1",
        currentCount = currentCount,
        goalCount = goalCount,
        history = emptyList(),
    )

private fun sampleCoupon(
    id: String = "coupon-1",
    title: String = "무료 음료 1잔 쿠폰",
    type: CouponType = CouponType.FreeDrink,
    amount: Int? = null,
    expiresAtMillis: Long = 1_809_331_200_000L,
    status: CouponStatus = CouponStatus.Available,
): Coupon =
    Coupon(
        id = id,
        type = type,
        title = title,
        amount = amount,
        expiresAtMillis = expiresAtMillis,
        status = status,
    )

private fun authenticatedUser(): AuthState =
    AuthState.Authenticated(
        UserProfile(
            id = "user-1",
            displayName = "민수",
            phoneLast4 = "1234",
        ),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class CouponMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
