package com.cafeminsu.ui.feature.gift

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.GiftRepository
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
class GiftViewModelTest {
    @get:Rule
    val mainDispatcherRule = GiftMainDispatcherRule()

    @Test
    fun initialFormSelectsTenThousandWonAndKakaoTalk() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(10_000, content.selectedAmount)
            assertEquals("10,000", content.selectedAmountLabel)
            assertEquals(GiftChannel.KakaoTalk, content.selectedChannel)
            assertEquals("구매하고 선물 보내기 · 10,000원", content.primaryButtonText)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun amountAndChannelSelectionUpdatesFormState() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitContent()

            viewModel.onAmountSelected(GiftAmountOption.TwentyThousand)
            val amountUpdated = awaitContent()
            assertEquals(20_000, amountUpdated.selectedAmount)
            assertEquals(GiftAmountOption.TwentyThousand, amountUpdated.selectedAmountOption)

            viewModel.onChannelSelected(GiftChannel.Sms)
            val channelUpdated = awaitContent()
            assertEquals(GiftChannel.Sms, channelUpdated.selectedChannel)
            assertEquals("연락처 입력", channelUpdated.recipientPlaceholder)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun successfulSendUsesRepositoryAndDoesNotExposeRecipientInEvent() = runTest {
        val recipient = "010-1234-5678"
        val giftRepository = FakeGiftRepository(
            result = AppResult.Success(
                GiftSendResult(
                    giftId = "gift-1",
                    sentAtMillis = 1_803_974_400_000L,
                ),
            ),
        )
        val viewModel = viewModel(giftRepository = giftRepository)

        viewModel.uiState.test {
            awaitContent()
            viewModel.onRecipientChanged(recipient)
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val sent = awaitItem()
                assertTrue(sent is GiftEvent.SendSucceeded)
                assertFalse(sent.message.contains(recipient))
                assertEquals(recipient, giftRepository.requests.single().recipientRef)
                assertEquals(10_000, giftRepository.requests.single().amount)
                assertEquals(GiftChannel.KakaoTalk, giftRepository.requests.single().channel)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendFailureProducesFailureEventWithoutRecipient() = runTest {
        val recipient = "friend-sensitive-id"
        val viewModel = viewModel(
            giftRepository = FakeGiftRepository(
                result = AppResult.Failure(DomainError.Network),
            ),
        )

        viewModel.uiState.test {
            awaitContent()
            viewModel.onRecipientChanged(recipient)
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val failed = awaitItem()
                assertTrue(failed is GiftEvent.SendFailed)
                assertFalse(failed.message.contains(recipient))

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankRecipientDoesNotSendGift() = runTest {
        val giftRepository = FakeGiftRepository(
            result = AppResult.Success(
                GiftSendResult(
                    giftId = "gift-1",
                    sentAtMillis = 1_803_974_400_000L,
                ),
            ),
        )
        val viewModel = viewModel(giftRepository = giftRepository)

        viewModel.events.test {
            viewModel.sendGift()

            val failed = awaitItem()
            assertTrue(failed is GiftEvent.SendFailed)
            assertTrue(giftRepository.requests.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestAuthStateProducesNeedsLoginState() = runTest {
        val viewModel = viewModel(
            sessionRepository = FakeGiftSessionRepository(AuthState.Guest),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        giftRepository: FakeGiftRepository = FakeGiftRepository(
            result = AppResult.Success(
                GiftSendResult(
                    giftId = "gift-1",
                    sentAtMillis = 1_803_974_400_000L,
                ),
            ),
        ),
        sessionRepository: FakeGiftSessionRepository = FakeGiftSessionRepository(authenticatedUser()),
    ): GiftViewModel =
        GiftViewModel(
            giftRepository = giftRepository,
            sessionRepository = sessionRepository,
        )

    private suspend fun ReceiveTurbine<GiftUiState>.awaitSettledState(): GiftUiState {
        val state = awaitItem()
        return if (state == GiftUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<GiftUiState>.awaitContent(): GiftUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is GiftUiState.Content)
        return state as GiftUiState.Content
    }

    private suspend fun ReceiveTurbine<GiftUiState>.awaitNeedsLogin(): GiftUiState.NeedsLogin {
        val state = awaitSettledState()
        assertTrue(state is GiftUiState.NeedsLogin)
        return state as GiftUiState.NeedsLogin
    }
}

private class FakeGiftRepository(
    private val result: AppResult<GiftSendResult>,
) : GiftRepository {
    val requests = mutableListOf<GiftSendRequest>()

    override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> {
        requests += request
        return result
    }
}

private class FakeGiftSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun authenticatedUser(): AuthState =
    AuthState.Authenticated(
        UserProfile(
            id = "user-1",
            displayName = "민수",
            phoneLast4 = "1234",
        ),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class GiftMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
