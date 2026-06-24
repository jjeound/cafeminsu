package com.cafeminsu.core.model.order

enum class OrderStatus {
    All,
    PendingPayment,
    Paid,
    Accepted,
    Preparing,
    Ready,
    Completed,
    Cancelled,
    Failed,
}
