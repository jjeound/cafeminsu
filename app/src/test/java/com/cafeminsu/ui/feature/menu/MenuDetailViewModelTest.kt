package com.cafeminsu.ui.feature.menu

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.ui.navigation.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectingOptionsAndQuantityRecalculatesUnitAndTotalPrice() = runTest {
        val viewModel = viewModel(menu = sampleMenu())

        viewModel.uiState.test {
            val initial = awaitContent()
            assertEquals(5_500, initial.unitPrice)
            assertEquals(5_500, initial.totalPrice)

            viewModel.onOptionToggle(groupId = "temperature", optionId = "temperature-ice")
            assertEquals(5_500, awaitContent().unitPrice)
            viewModel.onOptionToggle(groupId = "size", optionId = "size-large")
            assertEquals(6_000, awaitContent().unitPrice)

            viewModel.onOptionToggle(groupId = "shot", optionId = "shot-one")
            val selected = awaitContent()
            assertEquals(6_500, selected.unitPrice)
            assertEquals(6_500, selected.totalPrice)

            viewModel.onQuantityChange(2)
            val quantityChanged = awaitContent()
            assertEquals(2, quantityChanged.quantity)
            assertEquals(6_500, quantityChanged.unitPrice)
            assertEquals(13_000, quantityChanged.totalPrice)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requiredGroupsGateAddToCartAvailability() = runTest {
        val viewModel = viewModel(menu = sampleMenu())

        viewModel.uiState.test {
            assertFalse(awaitContent().canAddToCart)

            viewModel.onOptionToggle(groupId = "size", optionId = "size-large")
            assertFalse(awaitContent().canAddToCart)

            viewModel.onOptionToggle(groupId = "temperature", optionId = "temperature-ice")
            assertTrue(awaitContent().canAddToCart)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun quantityStepperIsBoundedToSafeRange() = runTest {
        val viewModel = viewModel(menu = sampleMenu())

        viewModel.uiState.test {
            awaitContent()

            viewModel.onQuantityChange(21)
            val maximum = awaitContent()
            assertEquals(20, maximum.quantity)
            assertEquals(110_000, maximum.totalPrice)

            viewModel.onQuantityChange(0)
            val minimum = awaitContent()
            assertEquals(1, minimum.quantity)
            assertEquals(5_500, minimum.totalPrice)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addToCartCallsRepositoryWithSelectedOptionsAndReportsSuccess() = runTest {
        val cartRepository = FakeCartRepository()
        val viewModel = viewModel(
            menu = sampleMenu(id = "latte"),
            cartRepository = cartRepository,
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.onOptionToggle(groupId = "temperature", optionId = "temperature-ice")
            awaitContent()
            viewModel.onOptionToggle(groupId = "size", optionId = "size-large")
            awaitContent()
            viewModel.onOptionToggle(groupId = "shot", optionId = "shot-two")
            awaitContent()
            viewModel.onQuantityChange(2)
            awaitContent()

            viewModel.onAddToCart()

            val added = awaitContent()
            assertEquals(MenuDetailAddStatus.Added, added.addStatus)
            assertEquals("latte", cartRepository.lastRequest?.menuItemId)
            assertEquals(2, cartRepository.lastRequest?.quantity)
            assertEquals(
                listOf("temperature-ice", "size-large", "shot-two"),
                cartRepository.lastRequest?.options?.map { it.optionId },
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addToCartFailureIsReflectedInState() = runTest {
        val viewModel = viewModel(
            menu = sampleMenu(),
            cartRepository = FakeCartRepository(addResult = AppResult.Failure(DomainError.Validation("options"))),
        )

        viewModel.uiState.test {
            awaitContent()

            viewModel.onOptionToggle(groupId = "temperature", optionId = "temperature-ice")
            awaitContent()
            viewModel.onOptionToggle(groupId = "size", optionId = "size-large")
            awaitContent()
            viewModel.onAddToCart()

            val failed = awaitContent()
            assertEquals(
                MenuDetailAddStatus.Error("입력값을 확인해 주세요"),
                failed.addStatus,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun soldOutMenuCannotBeAddedToCart() = runTest {
        val cartRepository = FakeCartRepository()
        val viewModel = viewModel(
            menu = sampleMenu(isSoldOut = true),
            cartRepository = cartRepository,
        )

        viewModel.uiState.test {
            val initial = awaitContent()
            assertFalse(initial.canAddToCart)

            viewModel.onAddToCart()

            val blocked = awaitContent()
            assertEquals(
                MenuDetailAddStatus.Error("품절된 메뉴는 담을 수 없어요"),
                blocked.addStatus,
            )
            assertEquals(null, cartRepository.lastRequest)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingMenuItemIdShowsError() = runTest {
        val viewModel = viewModel(
            menuItemId = "missing",
            menuResult = AppResult.Failure(DomainError.NotFound),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()
            assertTrue(state is MenuDetailUiState.Error)
            val error = state as MenuDetailUiState.Error
            assertEquals("메뉴 정보를 찾지 못했어요", error.message)
            assertFalse(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        menuItemId: String = "americano",
        menu: MenuItem = sampleMenu(id = menuItemId),
        menuResult: AppResult<MenuItem> = AppResult.Success(menu),
        cartRepository: FakeCartRepository = FakeCartRepository(),
    ): MenuDetailViewModel =
        MenuDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(Routes.MENU_DETAIL_MENU_ID to menuItemId),
            ),
            menuRepository = FakeMenuDetailMenuRepository(menuResult),
            cartRepository = cartRepository,
        )

    private suspend fun ReceiveTurbine<MenuDetailUiState>.awaitSettledState(): MenuDetailUiState {
        val state = awaitItem()
        return if (state == MenuDetailUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<MenuDetailUiState>.awaitContent(): MenuDetailUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is MenuDetailUiState.Content)
        return state as MenuDetailUiState.Content
    }
}

private class FakeMenuDetailMenuRepository(
    private val menuResult: AppResult<MenuItem>,
) : MenuRepository {
    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        MutableStateFlow(
            when (menuResult) {
                is AppResult.Success -> AppResult.Success(listOf(menuResult.data))
                is AppResult.Failure -> AppResult.Failure(menuResult.error)
            },
        )

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> = menuResult

    override suspend fun refreshMenus(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeCartRepository(
    private val addResult: AppResult<Cart> = AppResult.Success(emptyCart()),
) : CartRepository {
    var lastRequest: AddRequest? = null
        private set

    override fun observeCart(): Flow<AppResult<Cart>> = MutableStateFlow(AppResult.Success(emptyCart()))

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> {
        lastRequest = AddRequest(
            menuItemId = menuItemId,
            options = options,
            quantity = quantity,
        )
        return addResult
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> =
        AppResult.Success(emptyCart())

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> =
        AppResult.Success(emptyCart())

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        AppResult.Success(CartValidation.Invalid(listOf(CartInvalidReason.Empty)))

    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)
}

private data class AddRequest(
    val menuItemId: String,
    val options: List<SelectedOption>,
    val quantity: Int,
)

private fun sampleMenu(
    id: String = "americano",
    isSoldOut: Boolean = false,
): MenuItem =
    MenuItem(
        id = id,
        categoryId = "coffee",
        name = "바닐라라떼",
        description = "달콤한 바닐라 시럽이 어우러진 부드러운 라떼",
        basePrice = 5_500,
        imageUrl = null,
        isSoldOut = isSoldOut,
        options = listOf(
            MenuOptionGroup(
                id = "temperature",
                name = "온도",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    MenuOption(id = "temperature-hot", name = "HOT", extraPrice = 0, isAvailable = true),
                    MenuOption(id = "temperature-ice", name = "ICE", extraPrice = 0, isAvailable = true),
                ),
            ),
            MenuOptionGroup(
                id = "size",
                name = "사이즈",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    MenuOption(id = "size-regular", name = "Regular", extraPrice = 0, isAvailable = true),
                    MenuOption(id = "size-large", name = "Large", extraPrice = 500, isAvailable = true),
                ),
            ),
            MenuOptionGroup(
                id = "shot",
                name = "샷 추가",
                required = false,
                minSelect = 0,
                maxSelect = 1,
                options = listOf(
                    MenuOption(id = "shot-none", name = "없음", extraPrice = 0, isAvailable = true),
                    MenuOption(id = "shot-one", name = "+1샷", extraPrice = 500, isAvailable = true),
                    MenuOption(id = "shot-two", name = "+2샷", extraPrice = 1_000, isAvailable = true),
                ),
            ),
        ),
    )

private fun emptyCart(): Cart =
    Cart(
        items = emptyList<CartItem>(),
        subtotal = 0,
        minimumOrderAmount = 10_000,
        validation = CartValidation.Invalid(listOf(CartInvalidReason.Empty)),
    )
