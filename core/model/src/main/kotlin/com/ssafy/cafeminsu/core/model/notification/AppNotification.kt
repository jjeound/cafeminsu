package com.ssafy.cafeminsu.core.model.notification

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean,
)
