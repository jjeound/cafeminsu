package com.cafeminsu.core.model.order

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
