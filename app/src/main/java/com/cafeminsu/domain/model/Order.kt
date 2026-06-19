package com.cafeminsu.domain.model

data class Order(
    val id: String,
    val orderNumber: String,
    val items: List<CartItem>,
    val totalAmount: Int,
    val status: OrderStatus,
    val createdAtMillis: Long,
)

enum class OrderStatus {
    PendingPayment,
    Paid,
    Accepted,
    Preparing,
    Ready,
    Completed,
    Cancelled,
    Failed,
}

data class PaymentRequest(
    val orderId: String,
    val amount: Int,
    val paymentMethodToken: String,
    val idempotencyKey: String,
)

data class PaymentResult(
    val orderId: String,
    val paymentId: String,
    val status: PaymentStatus,
    val approvedAtMillis: Long?,
)

enum class PaymentStatus {
    Pending,
    Approved,
    Failed,
    Cancelled,
    Unknown,
}
