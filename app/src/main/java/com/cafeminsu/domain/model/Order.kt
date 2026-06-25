package com.cafeminsu.domain.model

data class Order(
    val id: String,
    val orderNumber: String,
    val items: List<CartItem>,
    val totalAmount: Int,
    val status: OrderStatus,
    val createdAtMillis: Long,
    // 서버 주문 응답의 매장명. 목록/상세 모두 제공하며, 미제공(생성 응답 등) 시 빈 문자열.
    val storeName: String = "",
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
    // 사용할 기프티콘 ID. 서버가 prepare 시점에 이 기프티콘으로 금액을 차감한다(전액 차감 시 즉시 PAID).
    val useGifticonId: Long? = null,
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
