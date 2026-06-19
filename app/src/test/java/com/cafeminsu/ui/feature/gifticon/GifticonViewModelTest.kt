package com.cafeminsu.ui.feature.gifticon

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
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
class GifticonViewModelTest {
    @get:Rule
    val mainDispatcherRule = GifticonListMainDispatcherRule()

    @Test
    fun authenticatedGifticonsProduceContentState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeGifticonListRewardRepository(
                AppResult.Success(
                    listOf(sampleGifticon(id = "gifticon-1")),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("gifticon-1", content.gifticons.single().id)
            assertEquals("아메리카노 교환권", content.gifticons.single().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyGifticonsProduceEmptyState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeGifticonListRewardRepository(
                AppResult.Success(emptyList()),
            ),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()
            assertEquals("보유한 기프티콘이 없어요", empty.message)
            assertEquals("스탬프 보러가기", empty.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rewardFailureProducesErrorState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeGifticonListRewardRepository(
                AppResult.Failure(DomainError.Network),
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
            sessionRepository = FakeGifticonListSessionRepository(AuthState.Guest),
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
            sessionRepository = FakeGifticonListSessionRepository(AuthState.Expired),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        rewardRepository: FakeGifticonListRewardRepository = FakeGifticonListRewardRepository(
            AppResult.Success(listOf(sampleGifticon())),
        ),
        sessionRepository: FakeGifticonListSessionRepository = FakeGifticonListSessionRepository(
            authenticatedUser(),
        ),
    ): GifticonViewModel =
        GifticonViewModel(
            rewardRepository = rewardRepository,
            sessionRepository = sessionRepository,
        )

    private suspend fun ReceiveTurbine<GifticonListUiState>.awaitSettledState(): GifticonListUiState {
        val state = awaitItem()
        return if (state == GifticonListUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<GifticonListUiState>.awaitContent(): GifticonListUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is GifticonListUiState.Content)
        return state as GifticonListUiState.Content
    }

    private suspend fun ReceiveTurbine<GifticonListUiState>.awaitEmpty(): GifticonListUiState.Empty {
        val state = awaitSettledState()
        assertTrue(state is GifticonListUiState.Empty)
        return state as GifticonListUiState.Empty
    }

    private suspend fun ReceiveTurbine<GifticonListUiState>.awaitErrorState(): GifticonListUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is GifticonListUiState.Error)
        return state as GifticonListUiState.Error
    }

    private suspend fun ReceiveTurbine<GifticonListUiState>.awaitNeedsLogin(): GifticonListUiState.NeedsLogin {
        val state = awaitSettledState()
        assertTrue(state is GifticonListUiState.NeedsLogin)
        return state as GifticonListUiState.NeedsLogin
    }
}

private class FakeGifticonListRewardRepository(
    initialGifticons: AppResult<List<Gifticon>>,
) : RewardRepository {
    private val gifticons = MutableStateFlow(initialGifticons)

    override fun observeStampCard(): Flow<AppResult<StampCard>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        AppResult.Failure(DomainError.NotFound)

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> = gifticons

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        AppResult.Failure(DomainError.NotFound)
}

private class FakeGifticonListSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun sampleGifticon(
    id: String = "gifticon-1",
    title: String = "아메리카노 교환권",
    status: GifticonStatus = GifticonStatus.Available,
): Gifticon =
    Gifticon(
        id = id,
        title = title,
        barcodeValue = "CAFE-MINSU-GIFT-0001",
        qrValue = "CAFE-MINSU-QR-0001",
        expiresAtMillis = 1_830_297_600_000L,
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
class GifticonListMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
