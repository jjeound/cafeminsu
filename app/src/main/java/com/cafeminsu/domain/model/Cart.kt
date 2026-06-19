package com.cafeminsu.domain.model

data class Cart(
    val items: List<CartItem>,
    val subtotal: Int,
    val minimumOrderAmount: Int,
    val validation: CartValidation,
    val orderType: OrderType = OrderType.DineIn,
    val requestNote: String? = null,
)

enum class OrderType {
    DineIn,
    Takeout,
}

data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)

data class SelectedOption(
    val groupId: String,
    val optionId: String,
    val name: String,
    val extraPrice: Int,
)

sealed interface CartValidation {
    data object Valid : CartValidation
    data class Invalid(val reasons: List<CartInvalidReason>) : CartValidation
}

sealed interface CartInvalidReason {
    data object Empty : CartInvalidReason
    data class BelowMinimumAmount(val shortage: Int) : CartInvalidReason
    data class SoldOut(val menuItemId: String) : CartInvalidReason
    data class PriceChanged(val menuItemId: String, val latestPrice: Int) : CartInvalidReason
    data class OptionUnavailable(val optionId: String) : CartInvalidReason
    data object StoreClosed : CartInvalidReason
}
