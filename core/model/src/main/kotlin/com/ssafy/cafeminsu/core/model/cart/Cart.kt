package com.ssafy.cafeminsu.core.model.cart

data class Cart(
    val items: List<CartItem>,
    val subtotal: Int,
    val validation: CartValidation,
    val orderType: OrderType = OrderType.DineIn,
    val requestNote: String = "",
)
