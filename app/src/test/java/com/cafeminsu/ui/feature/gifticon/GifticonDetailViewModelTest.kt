package com.cafeminsu.ui.feature.gifticon

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.navigation.Routes
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
class GifticonDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = GifticonDetailMainDispatcherRule()

    @Test
    fun detailLoadsGifticonFromSavedStateId() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeGifticonDetailRewardRepository(
                gifticon = sampleDetailGifticon(id = "gifticon-1"),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("gifticon-1", content.gifticon.id)
            assertEquals("아메리카노 교환권", content.gifticon.title)
            assertTrue(content.canUse)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onUseMarksAvailableGifticonUsedAndUpdatesState() = runTest {
        val rewardRepository = FakeGifticonDetailRewardRepository(
            gifticon = sampleDetailGifticon(status = GifticonStatus.Available),
        )
        val viewModel = viewModel(rewardRepository = rewardRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.onUse()

            val used = awaitContent()
            assertEquals(GifticonStatus.Used, used.gifticon.status)
            assertFalse(used.canUse)
            assertEquals("기프티콘을 사용 처리했어요", used.message)
            assertEquals(listOf("gifticon-1"), rewardRepository.markUsedCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun usedGifticonCannotBeUsedAgain() = runTest {
        val rewardRepository = FakeGifticonDetailRewardRepository(
            gifticon = sampleDetailGifticon(status = GifticonStatus.Used),
        )
        val viewModel = viewModel(rewardRepository = rewardRepository)

        viewModel.uiState.test {
            val content = awaitContent()
            assertFalse(content.canUse)

            viewModel.onUse()

            assertEquals(emptyList<String>(), rewardRepository.markUsedCalls)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun expiredGifticonCannotBeUsed() = runTest {
        val rewardRepository = FakeGifticonDetailRewardRepository(
            gifticon = sampleDetailGifticon(status = GifticonStatus.Expired),
        )
        val viewModel = viewModel(rewardRepository = rewardRepository)

        viewModel.uiState.test {
            val content = awaitContent()
            assertFalse(content.canUse)

            viewModel.onUse()

            assertEquals(emptyList<String>(), rewardRepository.markUsedCalls)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingGifticonIdShowsError() = runTest {
        val viewModel = viewModel(
            gifticonId = "missing",
            rewardRepository = FakeGifticonDetailRewardRepository(
                gifticonResult = AppResult.Failure(DomainError.NotFound),
            ),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()
            assertEquals("기프티콘을 찾지 못했어요", error.message)
            assertFalse(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        gifticonId: String = "gifticon-1",
        rewardRepository: FakeGifticonDetailRewardRepository = FakeGifticonDetailRewardRepository(
            gifticon = sampleDetailGifticon(id = gifticonId),
        ),
        sessionRepository: FakeGifticonDetailSessionRepository = FakeGifticonDetailSessionRepository(
            authenticatedDetailUser(),
        ),
    ): GifticonDetailViewModel =
        GifticonDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.GIFTICON_ID to gifticonId)),
            rewardRepository = rewardRepository,
            sessionRepository = sessionRepository,
        )

    private suspend fun ReceiveTurbine<GifticonDetailUiState>.awaitSettledState(): GifticonDetailUiState {
        val state = awaitItem()
        return if (state == GifticonDetailUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<GifticonDetailUiState>.awaitContent(): GifticonDetailUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is GifticonDetailUiState.Content)
        return state as GifticonDetailUiState.Content
    }

    private suspend fun ReceiveTurbine<GifticonDetailUiState>.awaitErrorState(): GifticonDetailUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is GifticonDetailUiState.Error)
        return state as GifticonDetailUiState.Error
    }
}

private class FakeGifticonDetailRewardRepository(
    gifticon: Gifticon = sampleDetailGifticon(),
    private val gifticonResult: AppResult<Gifticon> = AppResult.Success(gifticon),
) : RewardRepository {
    val markUsedCalls = mutableListOf<String>()

    override fun observeStampCard(): Flow<AppResult<StampCard>> =
        MutableStateFlow(AppResult.Failure(DomainError.NotFound))

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        AppResult.Failure(DomainError.NotFound)

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override suspend fun getGifticon(id: String): AppResult<Gifticon> = gifticonResult

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> {
        markUsedCalls += id
        return when (gifticonResult) {
            is AppResult.Success -> AppResult.Success(
                gifticonResult.data.copy(status = GifticonStatus.Used),
            )

            is AppResult.Failure -> gifticonResult
        }
    }
}

private class FakeGifticonDetailSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun sampleDetailGifticon(
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

private fun authenticatedDetailUser(): AuthState =
    AuthState.Authenticated(
        UserProfile(
            id = "user-1",
            displayName = "민수",
            phoneLast4 = "1234",
        ),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class GifticonDetailMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
