package com.ssafy.cafeminsu.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.cafeminsu.core.common.result.Result
import com.ssafy.cafeminsu.core.common.result.asResult
import com.ssafy.cafeminsu.core.data.repository.cart.CartRepository
import com.ssafy.cafeminsu.core.data.repository.menu.MenuRepository
import com.ssafy.cafeminsu.core.model.cart.Cart
import com.ssafy.cafeminsu.core.model.menu.MenuSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow(AllCategoryId)

    private val menusResult = menuRepository
        .getMenuSummaries(storeId = StoreId)
        .asResult()

    private val cartResult = cartRepository
        .getCartByStoreId(storeId = StoreId)
        .asResult()

    val uiState: StateFlow<MenuUiState> = combine(
        menusResult,
        cartResult,
        selectedCategoryId,
    ) { menusResult, cartResult, selectedCategoryId ->
        val menus = menusResult.dataOrNull().orEmpty()
        val cart = cartResult.dataOrNull()

        val categories = buildCategories(menus)

        val filteredMenus = if (selectedCategoryId == AllCategoryId) {
            menus
        } else {
            menus.filter { menu -> menu.category == selectedCategoryId }
        }

        MenuUiState(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            menus = filteredMenus.map { it.asUiModel() },
            cartCount = cart?.itemCount.orZero(),
            isLoading = menusResult is Result.Loading || cartResult is Result.Loading,
            errorMessage = listOf(menusResult, cartResult)
                .firstNotNullOfOrNull { result ->
                    (result as? Result.Error)?.exception?.message
                },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MenuUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                menuRepository.syncMenuSummaries(StoreId)
            }
        }
    }

    fun onCategoryClick(categoryId: String) {
        selectedCategoryId.update { categoryId }
    }

    private fun buildCategories(
        menus: List<MenuSummary>,
    ): List<MenuCategoryUiModel> {
        val menuCategories = menus
            .map { menu -> menu.category }
            .filter { category -> category.isNotBlank() }
            .distinct()
            .map { category ->
                MenuCategoryUiModel(
                    id = category,
                    label = category,
                )
            }

        return listOf(
            MenuCategoryUiModel(
                id = AllCategoryId,
                label = "전체",
            ),
        ) + menuCategories
    }

    private fun MenuSummary.asUiModel(): MenuUiModel =
        MenuUiModel(
            id = id,
            name = name,
            description = category,
            priceLabel = price.toPriceLabel(),
            categoryId = category,
            soldOut = !isAvailable,
        )

    private fun Int.toPriceLabel(): String =
        "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

    private fun Int?.orZero(): Int = this ?: 0

    private val Cart.itemCount: Int
        get() = items.sumOf { item -> item.quantity }

    private fun <T> Result<T>.dataOrNull(): T? =
        when (this) {
            is Result.Success -> data
            else -> null
        }

    private companion object {
        private const val StoreId = 1L
        private const val AllCategoryId = "all"
    }
}

data class MenuUiState(
    val categories: List<MenuCategoryUiModel> = listOf(
        MenuCategoryUiModel(
            id = "all",
            label = "전체",
        ),
    ),
    val selectedCategoryId: String = "all",
    val menus: List<MenuUiModel> = emptyList(),
    val cartCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class MenuCategoryUiModel(
    val id: String,
    val label: String,
)

data class MenuUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val priceLabel: String,
    val categoryId: String,
    val soldOut: Boolean = false,
)