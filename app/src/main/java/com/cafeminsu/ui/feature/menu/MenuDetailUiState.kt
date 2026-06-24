package com.cafeminsu.ui.feature.menu

sealed interface MenuDetailUiState {
    data object Loading : MenuDetailUiState

    data class Content(
        val menuItemId: String,
        val name: String,
        val description: String,
        val basePrice: Int,
        val isSoldOut: Boolean,
        val imageUrl: String? = null,
        val optionGroups: List<MenuDetailOptionGroupUiModel>,
        val selectedOptionIdsByGroup: Map<String, Set<String>>,
        val quantity: Int,
        val unitPrice: Int,
        val totalPrice: Int,
        val canAddToCart: Boolean,
        val addStatus: MenuDetailAddStatus,
        val isEditing: Boolean = false,
    ) : MenuDetailUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : MenuDetailUiState
}

data class MenuDetailOptionGroupUiModel(
    val id: String,
    val name: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val selectionMode: MenuDetailSelectionMode,
    val selectedOptionIds: Set<String>,
    val options: List<MenuDetailOptionUiModel>,
    val isSatisfied: Boolean,
    val helperText: String,
)

data class MenuDetailOptionUiModel(
    val id: String,
    val name: String,
    val extraPrice: Int,
    val isAvailable: Boolean,
    val selected: Boolean,
)

enum class MenuDetailSelectionMode {
    Single,
    Multiple,
}

sealed interface MenuDetailAddStatus {
    data object Idle : MenuDetailAddStatus
    data object Added : MenuDetailAddStatus
    data class Error(val message: String) : MenuDetailAddStatus
}

sealed interface MenuDetailEvent {
    data object AddedToCart : MenuDetailEvent
}
