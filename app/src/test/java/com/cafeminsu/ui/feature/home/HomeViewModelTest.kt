package com.cafeminsu.ui.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.MenuRepository
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
    fun repositoriesWithDataProduceContentState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(listOf(sampleMenu()))),
            rewardRepository = FakeRewardRepository(AppResult.Success(sampleStampCard())),
            sessionRepository = FakeSessionRepository(
                AuthState.Authenticated(
                    UserProfile(
                        id = "user-1",
                        displayName = "민수",
                        phoneLast4 = "1234",
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Content)
            val content = state as HomeUiState.Content
            assertEquals("민수님, 오늘도 카페민수와 함께해요", content.greeting)
            assertEquals("menu-1", content.recommendedMenus.single().id)
            assertEquals(4, content.stampSummary.currentCount)
            assertEquals(10, content.stampSummary.goalCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Failure(DomainError.Network)),
            rewardRepository = FakeRewardRepository(AppResult.Success(sampleStampCard())),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
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
    fun emptyMenusProduceEmptyState() = runTest {
        val viewModel = HomeViewModel(
            menuRepository = FakeMenuRepository(AppResult.Success(emptyList())),
            rewardRepository = FakeRewardRepository(AppResult.Success(sampleStampCard())),
            sessionRepository = FakeSessionRepository(AuthState.Guest),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is HomeUiState.Empty)
            val empty = state as HomeUiState.Empty
            assertEquals("어서 오세요, 카페민수입니다", empty.greeting)
            assertEquals("추천할 메뉴가 아직 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleMenu(): MenuItem =
        MenuItem(
            id = "menu-1",
            categoryId = "coffee",
            name = "민수 라떼",
            description = "고소한 우유와 진한 에스프레소",
            basePrice = 5200,
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

    private suspend fun ReceiveTurbine<HomeUiState>.awaitSettledState(): HomeUiState {
        val state = awaitItem()
        return if (state == HomeUiState.Loading) {
            awaitItem()
        } else {
            state
        }
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

private class FakeRewardRepository(
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
