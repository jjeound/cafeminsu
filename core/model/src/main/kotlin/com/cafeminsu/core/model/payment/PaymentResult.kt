package com.cafeminsu.core.model.payment

data class PaymentResult(
    val orderId: String,
    val paymentId: String,
    val status: PaymentStatus,
    val approvedAtMillis: Long?,
)
