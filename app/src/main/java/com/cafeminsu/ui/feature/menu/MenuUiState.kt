package com.cafeminsu.ui.feature.menu

sealed interface MenuUiState {
    data object Loading : MenuUiState

    data class Content(
        val categories: List<MenuCategoryUiModel>,
        val selectedCategoryId: String,
        val menus: List<MenuItemUiModel>,
    ) : MenuUiState

    data class Empty(
        val categories: List<MenuCategoryUiModel>,
        val selectedCategoryId: String?,
        val message: String,
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
)
