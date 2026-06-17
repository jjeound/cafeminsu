package com.cafeminsu.ui.feature.menu

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.MenuRepository
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
class MenuViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoadShowsCategoriesAndFirstCategoryMenus() = runTest {
        val repository = FakeMenuRepository(
            categories = AppResult.Success(sampleCategories()),
            menusByCategory = mapOf(
                "coffee" to AppResult.Success(
                    listOf(sampleMenu(id = "americano", categoryId = "coffee")),
                ),
                "tea" to AppResult.Success(
                    listOf(sampleMenu(id = "yuja-tea", categoryId = "tea")),
                ),
            ),
        )
        val viewModel = MenuViewModel(repository)

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuUiState.Content)
            val content = state as MenuUiState.Content
            assertEquals(listOf("coffee", "tea"), content.categories.map { it.id })
            assertEquals("coffee", content.selectedCategoryId)
            assertEquals(listOf("americano"), content.menus.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingCategoryUpdatesMenus() = runTest {
        val repository = FakeMenuRepository(
            categories = AppResult.Success(sampleCategories()),
            menusByCategory = mapOf(
                "coffee" to AppResult.Success(
                    listOf(sampleMenu(id = "americano", categoryId = "coffee")),
                ),
                "tea" to AppResult.Success(
                    listOf(sampleMenu(id = "yuja-tea", categoryId = "tea")),
                ),
            ),
        )
        val viewModel = MenuViewModel(repository)

        viewModel.uiState.test {
            assertEquals("coffee", (awaitSettledState() as MenuUiState.Content).selectedCategoryId)

            viewModel.onCategorySelect("tea")

            val selectedState = awaitItem() as MenuUiState.Content
            assertEquals("tea", selectedState.selectedCategoryId)
            assertEquals(listOf("yuja-tea"), selectedState.menus.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failureProducesErrorState() = runTest {
        val viewModel = MenuViewModel(
            FakeMenuRepository(
                categories = AppResult.Failure(DomainError.Network),
                menusByCategory = emptyMap(),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuUiState.Error)
            val error = state as MenuUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun menuFailureProducesErrorState() = runTest {
        val viewModel = MenuViewModel(
            FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = mapOf("coffee" to AppResult.Failure(DomainError.Timeout)),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuUiState.Error)
            val error = state as MenuUiState.Error
            assertEquals("응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyCategoriesProduceEmptyState() = runTest {
        val viewModel = MenuViewModel(
            FakeMenuRepository(
                categories = AppResult.Success(emptyList()),
                menusByCategory = emptyMap(),
            ),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuUiState.Empty)
            val empty = state as MenuUiState.Empty
            assertEquals("표시할 카테고리가 아직 없어요", empty.message)
            assertTrue(empty.categories.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun soldOutMenuItemStaysInListWithFlag() = runTest {
        val viewModel = MenuViewModel(
            FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = mapOf(
                    "coffee" to AppResult.Success(
                        listOf(
                            sampleMenu(
                                id = "einspanner",
                                categoryId = "coffee",
                                isSoldOut = true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitSettledState() as MenuUiState.Content
            assertEquals("einspanner", content.menus.single().id)
            assertTrue(content.menus.single().isSoldOut)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleCategories(): List<MenuCategory> =
        listOf(
            MenuCategory(id = "coffee", name = "커피", sortOrder = 1),
            MenuCategory(id = "tea", name = "티", sortOrder = 2),
        )

    private fun sampleMenu(
        id: String,
        categoryId: String,
        isSoldOut: Boolean = false,
    ): MenuItem =
        MenuItem(
            id = id,
            categoryId = categoryId,
            name = "민수 메뉴",
            description = "카페민수 메뉴 설명",
            basePrice = 5_000,
            imageUrl = null,
            isSoldOut = isSoldOut,
            options = emptyList(),
        )

    private suspend fun ReceiveTurbine<MenuUiState>.awaitSettledState(): MenuUiState {
        val state = awaitItem()
        return if (state == MenuUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }
}

private class FakeMenuRepository(
    categories: AppResult<List<MenuCategory>>,
    private val menusByCategory: Map<String, AppResult<List<MenuItem>>>,
) : MenuRepository {
    private val categories = MutableStateFlow(categories)

    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> = categories

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        MutableStateFlow(
            categoryId
                ?.let { menusByCategory[it] }
                ?: AppResult.Success(menusByCategory.values.flatMap { result ->
                    when (result) {
                        is AppResult.Success -> result.data
                        is AppResult.Failure -> emptyList()
                    }
                }),
        )

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun refreshMenus(): AppResult<Unit> = AppResult.Success(Unit)
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
