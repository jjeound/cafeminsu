package com.cafeminsu.ui.feature.owner.menu

data class OwnerMenuAddUiState(
    val imageUri: String? = null,
    val category: OwnerMenuAddCategory = OwnerMenuAddCategory.Coffee,
    val name: String = "",
    val priceInput: String = "",
    val description: String = "",
    val onSale: Boolean = true,
    val optionGroups: List<OwnerMenuOptionGroupInput> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val trimmedName: String get() = name.trim()

    val price: Int? get() = priceInput.toIntOrNull()

    val isNameValid: Boolean get() = trimmedName.length in MinNameLength..MaxNameLength

    val isPriceValid: Boolean get() = (price ?: 0) > 0

    // 옵션은 선택 입력이지만, 추가된 그룹은 이름과 옵션이 모두 채워져야 한다.
    val areOptionsValid: Boolean get() = optionGroups.all { it.isValid }

    val canSubmit: Boolean get() = isNameValid && isPriceValid && areOptionsValid && !isSubmitting

    companion object {
        const val MinNameLength = 1
        const val MaxNameLength = 30
        const val MaxPriceLength = 7
    }
}

data class OwnerMenuOptionGroupInput(
    val id: String,
    val name: String = "",
    val options: List<OwnerMenuOptionInput> = emptyList(),
) {
    val trimmedName: String get() = name.trim()

    val isValid: Boolean
        get() = trimmedName.isNotEmpty() && options.isNotEmpty() && options.all { it.isValid }
}

data class OwnerMenuOptionInput(
    val id: String,
    val name: String = "",
    val priceInput: String = "",
) {
    val trimmedName: String get() = name.trim()

    val extraPrice: Int get() = priceInput.toIntOrNull() ?: 0

    val isValid: Boolean get() = trimmedName.isNotEmpty()
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
