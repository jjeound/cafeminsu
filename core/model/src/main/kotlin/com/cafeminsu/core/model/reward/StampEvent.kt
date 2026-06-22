package com.cafeminsu.core.model.reward

data class StampEvent(
    val id: String,
    val orderId: String,
    val count: Int,
    val createdAtMillis: Long,
)
