package com.cafeminsu.ui.feature.owner.menu

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import com.cafeminsu.domain.repository.OwnerMenuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
class OwnerMenuViewModelTest {
    @get:Rule
    val mainDispatcherRule: TestWatcher = OwnerMenuMainDispatcherRule()

    @Test
    fun defaultStateShowsAllCategoryMenus() = runTest {
        val viewModel = OwnerMenuViewModel(
            ownerMenuRepository = FakeOwnerMenuRepository(
                initialResult = AppResult.Success(sampleMenus()),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(OwnerMenuFilter.All, content.selectedFilter)
            assertEquals(listOf("전체", "커피", "논커피", "디저트"), content.filters.map { it.label })
            assertEquals(listOf("아메리카노", "바닐라라떼", "초코쿠키"), content.menus.map { it.name })
            assertEquals("판매중", content.menus.first().statusLabel)
            assertFalse(content.menus.first().isDimmed)
            assertEquals("품절", content.menus[2].statusLabel)
            assertTrue(content.menus[2].isDimmed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingCategoryFiltersMenus() = runTest {
        val viewModel = OwnerMenuViewModel(
            ownerMenuRepository = FakeOwnerMenuRepository(
                initialResult = AppResult.Success(sampleMenus()),
            ),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.selectFilter(OwnerMenuFilter.NonCoffee)
            val nonCoffee = awaitContent()

            assertEquals(OwnerMenuFilter.NonCoffee, nonCoffee.selectedFilter)
            assertEquals(listOf("바닐라라떼"), nonCoffee.menus.map { it.name })

            viewModel.selectFilter(OwnerMenuFilter.Dessert)
            val dessert = awaitContent()

            assertEquals(OwnerMenuFilter.Dessert, dessert.selectedFilter)
            assertEquals(listOf("초코쿠키"), dessert.menus.map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutAfterRepositorySuccessUpdatesSoldOutAndDimmedState() = runTest {
        val repository = FakeOwnerMenuRepository(
            initialResult = AppResult.Success(sampleMenus()),
        )
        val viewModel = OwnerMenuViewModel(ownerMenuRepository = repository)

        viewModel.uiState.test {
            val initial = awaitContent()
            assertFalse(initial.menus.first { it.id == "americano" }.isSoldOut)

            viewModel.setSoldOut("americano")
            val updated = awaitContent()
            val americano = updated.menus.first { it.id == "americano" }

            assertEquals("americano", repository.lastSoldOutMenuId)
            assertEquals(true, repository.lastSoldOutValue)
            assertTrue(americano.isSoldOut)
            assertTrue(americano.isDimmed)
            assertEquals("품절", americano.statusLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptySelectedCategoryProducesEmptyState() = runTest {
        val viewModel = OwnerMenuViewModel(
            ownerMenuRepository = FakeOwnerMenuRepository(
                initialResult = AppResult.Success(
                    listOf(sampleMenu(id = "americano", categoryId = "coffee")),
                ),
            ),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.selectFilter(OwnerMenuFilter.Dessert)
            val empty = awaitEmpty()

            assertEquals(OwnerMenuFilter.Dessert, empty.selectedFilter)
            assertEquals("선택한 카테고리에 메뉴가 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = OwnerMenuViewModel(
            ownerMenuRepository = FakeOwnerMenuRepository(
                initialResult = AppResult.Failure(DomainError.Network),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is OwnerMenuUiState.Error)
            val error = state as OwnerMenuUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutFailureProducesErrorState() = runTest {
        val repository = FakeOwnerMenuRepository(
            initialResult = AppResult.Success(sampleMenus()),
            setSoldOutResult = AppResult.Failure(DomainError.NotFound),
        )
        val viewModel = OwnerMenuViewModel(ownerMenuRepository = repository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.setSoldOut("americano")
            val error = awaitErrorState()

            assertEquals("메뉴 정보를 찾지 못했어요", error.message)
            assertFalse(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<OwnerMenuUiState>.awaitContent(): OwnerMenuUiState.Content {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerMenuUiState.Content -> return state
                OwnerMenuUiState.Loading -> Unit
                is OwnerMenuUiState.Empty -> error("Expected content but was $state")
                is OwnerMenuUiState.Error -> error("Expected content but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerMenuUiState>.awaitEmpty(): OwnerMenuUiState.Empty {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerMenuUiState.Empty -> return state
                OwnerMenuUiState.Loading -> Unit
                is OwnerMenuUiState.Content -> Unit
                is OwnerMenuUiState.Error -> error("Expected empty but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerMenuUiState>.awaitErrorState(): OwnerMenuUiState.Error {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerMenuUiState.Error -> return state
                OwnerMenuUiState.Loading,
                is OwnerMenuUiState.Content,
                is OwnerMenuUiState.Empty,
                -> Unit
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerMenuUiState>.awaitSettledState(): OwnerMenuUiState {
        val state = awaitItem()
        return if (state == OwnerMenuUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }
}

private class FakeOwnerMenuRepository(
    initialResult: AppResult<List<MenuItem>>,
    private val setSoldOutResult: AppResult<MenuItem>? = null,
) : OwnerMenuRepository {
    private val menus = MutableStateFlow(initialResult)
    var lastSoldOutMenuId: String? = null
        private set
    var lastSoldOutValue: Boolean? = null
        private set

    override fun observeManagedMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        menus.map { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    if (categoryId == null) {
                        result.data
                    } else {
                        result.data.filter { it.categoryId == categoryId }
                    },
                )

                is AppResult.Failure -> result
            }
        }

    override suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem> {
        lastSoldOutMenuId = menuItemId
        lastSoldOutValue = soldOut
        setSoldOutResult?.let { return it }

        val currentMenus = (menus.value as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.Unknown)
        val currentMenu = currentMenus.firstOrNull { it.id == menuItemId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updatedMenu = currentMenu.copy(isSoldOut = soldOut)
        menus.value = AppResult.Success(
            currentMenus.map { menu ->
                if (menu.id == menuItemId) updatedMenu else menu
            },
        )
        return AppResult.Success(updatedMenu)
    }

    override suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem> {
        val currentMenus = (menus.value as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.Unknown)
        val currentMenu = currentMenus.firstOrNull { it.id == menuItemId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updatedMenu = currentMenu.copy(isVisible = visible)
        menus.value = AppResult.Success(
            currentMenus.map { menu ->
                if (menu.id == menuItemId) updatedMenu else menu
            },
        )
        return AppResult.Success(updatedMenu)
    }

    override suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem> =
        AppResult.Failure(DomainError.Unknown)
}

private fun sampleMenus(): List<MenuItem> =
    listOf(
        sampleMenu(id = "americano", categoryId = "coffee", name = "아메리카노"),
        sampleMenu(id = "vanilla-latte", categoryId = "noncoffee", name = "바닐라라떼"),
        sampleMenu(id = "choco-cookie", categoryId = "dessert", name = "초코쿠키", isSoldOut = true),
    )

private fun sampleMenu(
    id: String,
    categoryId: String,
    name: String = "아메리카노",
    isSoldOut: Boolean = false,
): MenuItem =
    MenuItem(
        id = id,
        categoryId = categoryId,
        name = name,
        description = "",
        basePrice = 4_500,
        imageUrl = null,
        isSoldOut = isSoldOut,
        options = emptyList(),
    )

@OptIn(ExperimentalCoroutinesApi::class)
private class OwnerMenuMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
