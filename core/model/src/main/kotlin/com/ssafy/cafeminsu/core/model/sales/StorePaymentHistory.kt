package com.ssafy.cafeminsu.core.model.sales

data class StorePaymentHistory(val totalAmount: Int, val payments: List<StorePayment>)

data class StorePayment(
    val paymentId: Long,
    val orderId: Long,
    val method: String,
    val amount: Int,
    val paidAt: String,
)
