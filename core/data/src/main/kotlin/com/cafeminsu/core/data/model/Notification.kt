package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.notification.NotificationEntity
import com.cafeminsu.core.model.notification.AppNotification
import com.cafeminsu.core.model.notification.NotificationType
import com.cafeminsu.core.network.model.response.notification.NotificationResponse
import java.time.Instant

fun NotificationResponse.asEntity(): NotificationEntity =
    NotificationEntity(
        id = id,
        title = title,
        body = body,
        type = type,
        isRead = isRead,
        relatedEntityId = relatedEntityId,
        createdAtMillis = Instant.parse(createdAt).toEpochMilli(),
    )

fun NotificationResponse.asExternalModel() = AppNotification(
    id.toString(),
    type.asNotificationType(),
    title,
    body,
    Instant.parse(createdAt).toEpochMilli(),
    isRead
)

fun NotificationEntity.asExternalModel(): AppNotification =
    AppNotification(
        id = id.toString(),
        type = type.asNotificationType(),
        title = title,
        body = body,
        createdAtMillis = createdAtMillis,
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
