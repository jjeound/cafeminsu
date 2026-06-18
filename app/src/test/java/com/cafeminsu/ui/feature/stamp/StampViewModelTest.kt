package com.cafeminsu.ui.feature.stamp

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.StampEvent
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class StampViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun authenticatedStampCardProducesContentState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeRewardRepository(
                AppResult.Success(
                    sampleStampCard(
                        currentCount = 4,
                        history = listOf(sampleStampEvent(orderId = "order-1")),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(4, content.currentCount)
            assertEquals(10, content.goalCount)
            assertEquals("order-1", content.history.single().orderId)
            assertFalse(content.isGoalReached)
            assertEquals(0.4f, content.progress, 0.001f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyStampHistoryProducesEmptyState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeRewardRepository(
                AppResult.Success(
                    sampleStampCard(
                        currentCount = 4,
                        history = emptyList(),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()
            assertEquals("아직 적립 내역이 없어요", empty.message)
            assertEquals(4, empty.currentCount)
            assertEquals(10, empty.goalCount)
            assertFalse(empty.isGoalReached)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rewardFailureProducesErrorState() = runTest {
        val viewModel = viewModel(
            rewardRepository = FakeRewardRepository(AppResult.Failure(DomainError.Network)),
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
            sessionRepository = FakeSessionRepository(AuthState.Guest),
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
            sessionRepository = FakeSessionRepository(AuthState.Expired),
        )

        viewModel.uiState.test {
            val needsLogin = awaitNeedsLogin()
            assertEquals("로그인이 필요해요", needsLogin.message)
            assertEquals("다시 로그인하기", needsLogin.actionLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun stampCardUpdatesAreReflectedInContentState() = runTest {
        val rewardRepository = FakeRewardRepository(
            AppResult.Success(
                sampleStampCard(
                    currentCount = 4,
                    history = listOf(sampleStampEvent(orderId = "order-1")),
                ),
            ),
        )
        val viewModel = viewModel(rewardRepository = rewardRepository)

        viewModel.uiState.test {
            assertEquals(4, awaitContent().currentCount)

            rewardRepository.emit(
                AppResult.Success(
                    sampleStampCard(
                        currentCount = 5,
                        history = listOf(
                            sampleStampEvent(
                                id = "stamp-2",
                                orderId = "order-2",
                            ),
                            sampleStampEvent(orderId = "order-1"),
                        ),
                    ),
                ),
            )

            val updated = awaitContent()
            assertEquals(5, updated.currentCount)
            assertEquals(listOf("order-2", "order-1"), updated.history.map { it.orderId })

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        rewardRepository: FakeRewardRepository = FakeRewardRepository(
            AppResult.Success(
                sampleStampCard(history = listOf(sampleStampEvent())),
            ),
        ),
        sessionRepository: FakeSessionRepository = FakeSessionRepository(authenticatedUser()),
    ): StampViewModel =
        StampViewModel(
            rewardRepository = rewardRepository,
            sessionRepository = sessionRepository,
        )

    private fun sampleStampCard(
        currentCount: Int = 4,
        goalCount: Int = 10,
        history: List<StampEvent>,
    ): StampCard =
        StampCard(
            userId = "user-1",
            currentCount = currentCount,
            goalCount = goalCount,
            history = history,
        )

    private fun sampleStampEvent(
        id: String = "stamp-1",
        orderId: String = "order-1",
        count: Int = 1,
        createdAtMillis: Long = 1_803_974_400_000L,
    ): StampEvent =
        StampEvent(
            id = id,
            orderId = orderId,
            count = count,
            createdAtMillis = createdAtMillis,
        )

    private fun authenticatedUser(): AuthState =
        AuthState.Authenticated(
            UserProfile(
                id = "user-1",
                displayName = "민수",
                phoneLast4 = "1234",
            ),
        )

    private suspend fun ReceiveTurbine<StampUiState>.awaitSettledState(): StampUiState {
        val state = awaitItem()
        return if (state == StampUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<StampUiState>.awaitContent(): StampUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is StampUiState.Content)
        return state as StampUiState.Content
    }

    private suspend fun ReceiveTurbine<StampUiState>.awaitEmpty(): StampUiState.Empty {
        val state = awaitSettledState()
        assertTrue(state is StampUiState.Empty)
        return state as StampUiState.Empty
    }

    private suspend fun ReceiveTurbine<StampUiState>.awaitErrorState(): StampUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is StampUiState.Error)
        return state as StampUiState.Error
    }

    private suspend fun ReceiveTurbine<StampUiState>.awaitNeedsLogin(): StampUiState.NeedsLogin {
        val state = awaitSettledState()
        assertTrue(state is StampUiState.NeedsLogin)
        return state as StampUiState.NeedsLogin
    }
}

private class FakeRewardRepository(
    initialStampCard: AppResult<StampCard>,
) : RewardRepository {
    private val stampCard = MutableStateFlow(initialStampCard)

    fun emit(result: AppResult<StampCard>) {
        stampCard.value = result
    }

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

private class FakeSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

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
