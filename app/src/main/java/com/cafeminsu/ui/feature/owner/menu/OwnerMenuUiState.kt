package com.cafeminsu.ui.feature.owner.menu

sealed interface OwnerMenuUiState {
    data object Loading : OwnerMenuUiState

    data class Content(
        val selectedFilter: OwnerMenuFilter,
        val filters: List<OwnerMenuFilterUiModel>,
        val menus: List<OwnerMenuItemUiModel>,
    ) : OwnerMenuUiState

    data class Empty(
        val selectedFilter: OwnerMenuFilter,
        val filters: List<OwnerMenuFilterUiModel>,
        val message: String,
    ) : OwnerMenuUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : OwnerMenuUiState
}

enum class OwnerMenuFilter(
    val categoryId: String?,
    val label: String,
) {
    All(categoryId = null, label = "전체"),
    Coffee(categoryId = "coffee", label = "커피"),
    NonCoffee(categoryId = "noncoffee", label = "논커피"),
    Dessert(categoryId = "dessert", label = "디저트"),
}

data class OwnerMenuFilterUiModel(
    val filter: OwnerMenuFilter,
    val label: String,
    val selected: Boolean,
)

data class OwnerMenuItemUiModel(
    val id: String,
    val name: String,
    val price: Int,
    val isSoldOut: Boolean,
    val statusLabel: String,
    val isDimmed: Boolean,
    val isActionInProgress: Boolean,
)
