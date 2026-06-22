package com.cafeminsu.core.model.order

import com.cafeminsu.core.model.cart.CartItem

data class Order(
    val id: String,
    val orderNumber: String,
    val items: List<CartItem>,
    val totalAmount: Int,
    val status: OrderStatus,
    val createdAtMillis: Long,
)
