package com.cafeminsu.ui.feature.menu

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
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
        val viewModel = MenuViewModel(
            menuRepository = repository,
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuUiState.Content)
            val content = state as MenuUiState.Content
            assertEquals(
                listOf("recommendation", "coffee", "noncoffee", "dessert", "tea"),
                content.categories.map { it.id },
            )
            assertEquals("recommendation", content.selectedCategoryId)
            assertEquals(listOf("americano", "yuja-tea"), content.menus.map { it.id })

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
        val viewModel = MenuViewModel(
            menuRepository = repository,
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
        )

        viewModel.uiState.test {
            assertEquals("recommendation", (awaitSettledState() as MenuUiState.Content).selectedCategoryId)

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
            menuRepository = FakeMenuRepository(
                categories = AppResult.Failure(DomainError.Network),
                menusByCategory = emptyMap(),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
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
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = mapOf("coffee" to AppResult.Failure(DomainError.Timeout)),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
        )

        viewModel.uiState.test {
            awaitSettledState()
            viewModel.onCategorySelect("coffee")
            val state = awaitItem()
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
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(emptyList()),
                menusByCategory = emptyMap(),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
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
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = mapOf(
                    null to AppResult.Success(
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
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(),
        )

        viewModel.uiState.test {
            val content = awaitSettledState() as MenuUiState.Content
            assertEquals("einspanner", content.menus.single().id)
            assertTrue(content.menus.single().isSoldOut)
            assertEquals(false, content.menus.single().isEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectedStoreNameIsMappedForHeader() = runTest {
        val viewModel = MenuViewModel(
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = mapOf(
                    null to AppResult.Success(listOf(sampleMenu(id = "americano", categoryId = "coffee"))),
                ),
            ),
            storeRepository = FakeStoreRepository(
                selectedStore = sampleStore(name = "카페민수 강남점"),
            ),
            cartRepository = FakeMenuCartRepository(),
        )

        viewModel.uiState.test {
            val content = awaitSettledState() as MenuUiState.Content
            assertEquals("강남점", content.storeName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyCartProducesZeroCartItemCount() = runTest {
        val viewModel = MenuViewModel(
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = emptyMap(),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(cart = AppResult.Success(cartWithQuantities())),
        )

        viewModel.cartItemCount.test {
            assertEquals(0, awaitCount(0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cartItemCountSumsItemQuantities() = runTest {
        val viewModel = MenuViewModel(
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = emptyMap(),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(cart = AppResult.Success(cartWithQuantities(2, 3))),
        )

        viewModel.cartItemCount.test {
            assertEquals(5, awaitCount(5))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cartFailureProducesZeroCartItemCount() = runTest {
        val viewModel = MenuViewModel(
            menuRepository = FakeMenuRepository(
                categories = AppResult.Success(sampleCategories()),
                menusByCategory = emptyMap(),
            ),
            storeRepository = FakeStoreRepository(),
            cartRepository = FakeMenuCartRepository(cart = AppResult.Failure(DomainError.Unknown)),
        )

        viewModel.cartItemCount.test {
            assertEquals(0, awaitCount(0))
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

    private suspend fun ReceiveTurbine<Int>.awaitCount(expected: Int): Int {
        var value = awaitItem()
        while (value != expected) {
            value = awaitItem()
        }
        return value
    }
}

private class FakeMenuRepository(
    categories: AppResult<List<MenuCategory>>,
    private val menusByCategory: Map<String?, AppResult<List<MenuItem>>>,
) : MenuRepository {
    private val categories = MutableStateFlow(categories)

    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> = categories

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        MutableStateFlow(
            menusByCategory[categoryId]
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

private class FakeStoreRepository(
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

private class FakeMenuCartRepository(
    cart: AppResult<Cart> = AppResult.Success(
        Cart(items = emptyList(), subtotal = 0, validation = CartValidation.Valid),
    ),
) : CartRepository {
    private val cart = MutableStateFlow(cart)

    override fun observeCart(): Flow<AppResult<Cart>> = cart

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> = cart.value

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> = cart.value

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> = cart.value

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        AppResult.Success(CartValidation.Valid)

    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun cartWithQuantities(vararg quantities: Int): Cart =
    Cart(
        items = quantities.mapIndexed { index, quantity ->
            CartItem(
                id = "item-$index",
                menuItemId = "menu-$index",
                name = "민수 메뉴",
                unitPrice = 5_000,
                selectedOptions = emptyList(),
                quantity = quantity,
            )
        },
        subtotal = quantities.sum() * 5_000,
        validation = CartValidation.Valid,
    )

private fun sampleStore(name: String): Store =
    Store(
        id = "gangnam",
        name = name,
        address = "서울 강남구 테헤란로 134",
        phone = "02-3456-7890",
        distanceMeters = 120,
        latitude = 37.498,
        longitude = 127.028,
        status = StoreStatus.Open,
        closingTimeLabel = "22:00 마감",
        amenities = emptyList(),
    )

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
