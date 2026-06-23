package com.cafeminsu.domain.model

import com.squareup.moshi.JsonClass

data class Cart(
    val items: List<CartItem>,
    val subtotal: Int,
    val validation: CartValidation,
    val orderType: OrderType = OrderType.DineIn,
    val requestNote: String? = null,
)

enum class OrderType {
    DineIn,
    Takeout,
}

// 주문 내역 캐시 시 한 컬럼에 Moshi JSON 으로 직렬화하므로 codegen 어댑터를 생성한다(Menu 옵션과 동일 패턴).
@JsonClass(generateAdapter = true)
data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)

@JsonClass(generateAdapter = true)
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
    data class SoldOut(val menuItemId: String) : CartInvalidReason
    data class PriceChanged(val menuItemId: String, val latestPrice: Int) : CartInvalidReason
    data class OptionUnavailable(val optionId: String) : CartInvalidReason
    data object StoreClosed : CartInvalidReason
}
