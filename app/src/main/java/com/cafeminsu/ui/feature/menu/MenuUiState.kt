package com.cafeminsu.ui.feature.menu

sealed interface MenuUiState {
    data object Loading : MenuUiState

    data class Content(
        val categories: List<MenuCategoryUiModel>,
        val selectedCategoryId: String,
        val menus: List<MenuItemUiModel>,
        val storeName: String = DefaultMenuStoreName,
    ) : MenuUiState

    data class Empty(
        val categories: List<MenuCategoryUiModel>,
        val selectedCategoryId: String?,
        val message: String,
        val storeName: String = DefaultMenuStoreName,
    ) : MenuUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : MenuUiState
}

data class MenuCategoryUiModel(
    val id: String,
    val name: String,
)

data class MenuItemUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val isSoldOut: Boolean,
    val isEnabled: Boolean = !isSoldOut,
)

const val DefaultMenuStoreName = "강남점"
