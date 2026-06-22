package com.cafeminsu.core.model.cart

sealed interface CartInvalidReason {
    data object Empty : CartInvalidReason
    data class SoldOut(val menuItemId: String) : CartInvalidReason
    data class PriceChanged(val menuItemId: String, val latestPrice: Int) : CartInvalidReason
    data class OptionUnavailable(val optionId: String) : CartInvalidReason
    data object StoreClosed : CartInvalidReason
}
