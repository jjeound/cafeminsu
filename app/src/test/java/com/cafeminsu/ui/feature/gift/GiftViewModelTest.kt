package com.cafeminsu.ui.feature.gift

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
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
    fun initialFormSelectsTenThousandWon() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(10_000, content.selectedAmount)
            assertEquals("10,000", content.selectedAmountLabel)
            assertEquals("구매하고 선물 보내기 · 10,000원", content.primaryButtonText)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun amountSelectionUpdatesFormState() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitContent()

            viewModel.onAmountSelected(GiftAmountOption.TwentyThousand)
            val amountUpdated = awaitContent()
            assertEquals(20_000, amountUpdated.selectedAmount)
            assertEquals(GiftAmountOption.TwentyThousand, amountUpdated.selectedAmountOption)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendWithoutShareDataEmitsSendSucceeded() = runTest {
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

            viewModel.events.test {
                viewModel.sendGift()

                val sent = awaitItem()
                assertTrue(sent is GiftEvent.SendSucceeded)
                assertEquals(10_000, giftRepository.requests.single().amount)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendWithClaimCodeEmitsShareGiftLinkWithCodeAndNoCustomScheme() = runTest {
        val giftRepository = FakeGiftRepository(
            result = AppResult.Success(
                GiftSendResult(
                    giftId = "gift-1",
                    sentAtMillis = 1_803_974_400_000L,
                    claimCode = "GFT-1234-5678",
                ),
            ),
        )
        val viewModel = viewModel(giftRepository = giftRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val event = awaitItem()
                assertTrue(event is GiftEvent.ShareGiftLink)
                val share = event as GiftEvent.ShareGiftLink
                // 등록 코드만 안내하고, 카톡에서 열리지 않는 cafeminsu:// 커스텀 스킴은 넣지 않는다.
                assertTrue(share.shareText.contains("등록 코드: GFT-1234-5678"))
                assertFalse(share.shareText.contains("cafeminsu://"))

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendSharesClaimCodeOnlyEvenWhenServerShareLinkPresent() = runTest {
        val giftRepository = FakeGiftRepository(
            result = AppResult.Success(
                GiftSendResult(
                    giftId = "gift-1",
                    sentAtMillis = 1_803_974_400_000L,
                    shareLink = "https://cafeminsu.example/gift?code=GFT-1234-5678",
                    claimCode = "GFT-1234-5678",
                ),
            ),
        )
        val viewModel = viewModel(giftRepository = giftRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val event = awaitItem()
                assertTrue(event is GiftEvent.ShareGiftLink)
                val share = event as GiftEvent.ShareGiftLink
                // 링크는 일절 넣지 않고 등록 코드만 공유한다.
                assertTrue(share.shareText.contains("등록 코드: GFT-1234-5678"))
                assertFalse(share.shareText.contains("http"))
                assertFalse(share.shareText.contains("cafeminsu://"))

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendFailureProducesFailureEvent() = runTest {
        val viewModel = viewModel(
            giftRepository = FakeGiftRepository(
                result = AppResult.Failure(DomainError.Network),
            ),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val failed = awaitItem()
                assertTrue(failed is GiftEvent.SendFailed)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun zeroAmountDoesNotSendGift() = runTest {
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
            // 직접입력 선택 + 금액 미입력이면 전송 불가 → 전송 시도해도 실패 이벤트만 발생하고 호출 안 됨.
            viewModel.onAmountSelected(GiftAmountOption.Custom)
            awaitContent()

            viewModel.events.test {
                viewModel.sendGift()

                val failed = awaitItem()
                assertTrue(failed is GiftEvent.SendFailed)
                assertTrue(giftRepository.requests.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }

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

    override suspend fun claimGift(
        claimCode: String,
    ): AppResult<com.cafeminsu.domain.model.Gifticon> =
        AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)
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
