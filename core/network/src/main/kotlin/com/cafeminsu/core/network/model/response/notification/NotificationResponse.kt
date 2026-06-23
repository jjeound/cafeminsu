package com.cafeminsu.core.network.model.response.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.notification.AppNotification
import com.cafeminsu.core.model.notification.NotificationType
import java.time.Instant

@Serializable
data class NotificationResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "title") val title: String,
    @SerialName(value = "body") val body: String,
    @SerialName(value = "type") val type: String,
    @SerialName(value = "isRead") val isRead: Boolean,
    @SerialName(value = "relatedEntityId") val relatedEntityId: Long?,
    @SerialName(value = "createdAt") val createdAt: String,
)

fun NotificationResponse.asExternalModel(): AppNotification =
    AppNotification(
        id = id.toString(),
        type = type.asNotificationType(),
        title = title,
        body = body,
        createdAtMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L),
        read = isRead,
    )

private fun String.asNotificationType(): NotificationType =
    when (uppercase()) {
        "ORDER_ACCEPTED" -> NotificationType.OrderAccepted
        "ORDER_READY" -> NotificationType.OrderReady
        "ORDER_COMPLETED" -> NotificationType.OrderCompleted
        "STAMP", "STAMP_EARNED" -> NotificationType.StampEarned
        "GIFTICON", "GIFTICON_RECEIVED" -> NotificationType.GifticonReceived
        else -> NotificationType.System
    }
