package com.cafeminsu.core.model.order

data class OrderSummary(
    val id: Long,
    val orderNumber: String,
    val totalAmount: Int,
    val status: OrderStatus,
    val createdAtMillis: Long,
)
