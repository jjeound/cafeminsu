package com.cafeminsu.core.model.payment

data class PaymentRequest(
    val orderId: String,
    val amount: Int,
    val paymentMethodToken: String,
    val idempotencyKey: String,
)
