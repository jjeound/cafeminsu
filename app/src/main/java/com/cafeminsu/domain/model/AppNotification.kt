package com.cafeminsu.domain.model

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean,
)

enum class NotificationType {
    OrderAccepted,
    OrderReady,
    OrderCompleted,
    StampEarned,
    GifticonReceived,
}
