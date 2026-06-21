package com.cafeminsu.ui.feature.owner.menu

data class OwnerMenuAddUiState(
    val imageUri: String? = null,
    val category: OwnerMenuAddCategory = OwnerMenuAddCategory.Coffee,
    val name: String = "",
    val priceInput: String = "",
    val description: String = "",
    val onSale: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val trimmedName: String get() = name.trim()

    val price: Int? get() = priceInput.toIntOrNull()

    val isNameValid: Boolean get() = trimmedName.length in MinNameLength..MaxNameLength

    val isPriceValid: Boolean get() = (price ?: 0) > 0

    val canSubmit: Boolean get() = isNameValid && isPriceValid && !isSubmitting

    companion object {
        const val MinNameLength = 1
        const val MaxNameLength = 30
        const val MaxPriceLength = 7
    }
}

enum class OwnerMenuAddCategory(
    val categoryId: String,
    val label: String,
) {
    Coffee(categoryId = "coffee", label = "커피"),
    NonCoffee(categoryId = "noncoffee", label = "논커피"),
    Dessert(categoryId = "dessert", label = "디저트"),
}

sealed interface OwnerMenuAddEvent {
    data object Saved : OwnerMenuAddEvent

    data class ShowSnackbar(val message: String) : OwnerMenuAddEvent
}
