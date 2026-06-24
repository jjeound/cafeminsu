package com.cafeminsu.ui.feature.coupon

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.domain.repository.StoreRepository
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
import org.junit.Assert.assertNull
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
    fun authenticatedStateMapsStampProgressAndGifticonList() = runTest {
        val nowMillis = 1_803_974_400_000L
        val viewModel = viewModel(
            rewardRepository = FakeCouponRewardRepository(
                stampCard = AppResult.Success(sampleStampCard(currentCount = 7, goalCount = 10)),
                gifticons = AppResult.Success(
                    listOf(
                        sampleGifticon(id = "gifticon-free", title = "무료 음료 1잔 쿠폰"),
                        sampleGifticon(id = "gifticon-second", title = "민수 라떼 교환권"),
                    ),
                ),
            ),
            storeRepository = FakeCouponStoreRepository(sampleStore(name = "민수 강남점")),
            nowMillis = { nowMillis },
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("민수 강남점", content.stamp.storeName)
            assertEquals("7 / 10", content.stamp.countLabel)
            assertEquals(3, content.stamp.remainingCount)
            assertEquals(10, content.stamp.slots.size)
            assertEquals(7, content.stamp.slots.count { it.filled })
            assertEquals("8", content.stamp.slots[7].label)
            assertEquals("스탬프 3개만 더 모으면 무료 음료 쿠폰!", content.stamp.guideMessage)
            assertEquals(2, content.coupons.size)
            assertEquals("무료 음료 1잔 쿠폰", content.coupons.first().title)
            assertNull(content.coupons.first().amount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyGifticonListStillProducesContentWithStamp() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeCouponRewardRepository(
                stampCard = AppResult.Success(sampleStampCard()),
                gifticons = AppResult.Success(emptyList()),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertTrue(content.coupons.isEmpty())
            assertEquals("7 / 10", content.stamp.countLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingSelectedStoreFallsBackToNeutralStoreName() = runTest {
        val viewModel = viewModel(
            storeRepository = FakeCouponStoreRepository(selectedStore = null),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("마이 카페", content.stamp.storeName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun expiringUsedAndExpiredGifticonsExposeDisplayStatus() = runTest {
        val nowMillis = 1_803_974_400_000L
        val viewModel = viewModel(
            rewardRepository = FakeCouponRewardRepository(
                stampCard = AppResult.Success(sampleStampCard()),
                gifticons = AppResult.Success(
                    listOf(
                        sampleGifticon(
                            id = "soon",
                            expiresAtMillis = nowMillis + OneDayMillis,
                        ),
                        sampleGifticon(
                            id = "used",
                            status = GifticonStatus.Used,
                        ),
                        sampleGifticon(
                            id = "expired",
                            status = GifticonStatus.Expired,
                        ),
                    ),
                ),
            ),
            nowMillis = { nowMillis },
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertTrue(content.coupons.first { it.id == "soon" }.available)
            assertTrue(content.coupons.first { it.id == "soon" }.expiringSoon)
            assertFalse(content.coupons.first { it.id == "used" }.available)
            assertTrue(content.coupons.first { it.id == "used" }.dimmed)
            assertFalse(content.coupons.first { it.id == "expired" }.available)
            assertTrue(content.coupons.first { it.id == "expired" }.dimmed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun gifticonFailureProducesRetryableErrorState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeCouponRewardRepository(
                stampCard = AppResult.Success(sampleStampCard()),
                gifticons = AppResult.Failure(DomainError.Network),
            ),
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

    @Test
    fun expiredAuthStateProducesNeedsLoginState() = runTest {
        val viewModel = viewModel(
            sessionRepository = FakeCouponSessionRepository(AuthState.Expired),
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
            stampCard = AppResult.Success(sampleStampCard()),
            gifticons = AppResult.Success(listOf(sampleGifticon())),
        ),
        storeRepository: FakeCouponStoreRepository = FakeCouponStoreRepository(sampleStore()),
        sessionRepository: FakeCouponSessionRepository = FakeCouponSessionRepository(authenticatedUser()),
        nowMillis: () -> Long = { System.currentTimeMillis() },
    ): CouponViewModel =
        CouponViewModel(
            rewardRepository = rewardRepository,
            storeRepository = storeRepository,
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

private class FakeCouponStoreRepository(
    selectedStore: Store? = null,
) : StoreRepository {
    private val selectedStore = MutableStateFlow(selectedStore)

    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override suspend fun getStore(storeId: String): AppResult<Store> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun selectStore(storeId: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override fun observeSelectedStore(): Flow<Store?> = selectedStore
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

private fun sampleGifticon(
    id: String = "gifticon-1",
    title: String = "무료 음료 1잔 쿠폰",
    expiresAtMillis: Long = 1_809_331_200_000L,
    status: GifticonStatus = GifticonStatus.Available,
): Gifticon =
    Gifticon(
        id = id,
        title = title,
        barcodeValue = "barcode-$id",
        qrValue = "qr-$id",
        expiresAtMillis = expiresAtMillis,
        status = status,
    )

private fun sampleStore(
    name: String = "민수 강남점",
): Store =
    Store(
        id = "store-1",
        name = name,
        address = "서울특별시 강남구",
        phone = "02-000-0000",
        distanceMeters = 100,
        latitude = 37.0,
        longitude = 127.0,
        status = StoreStatus.Open,
        closingTimeLabel = null,
        amenities = emptyList(),
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
