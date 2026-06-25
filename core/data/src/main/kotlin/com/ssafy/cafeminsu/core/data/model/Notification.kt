package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.database.model.entity.notification.NotificationEntity
import com.ssafy.cafeminsu.core.model.notification.AppNotification
import com.ssafy.cafeminsu.core.model.notification.NotificationType
import com.ssafy.cafeminsu.core.network.model.response.notification.NotificationResponse
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

fun NotificationResponse.asExternalModel() =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.notification.AppNotification(
        id.toString(),
        type.asNotificationType(),
        title,
        body,
        Instant.parse(createdAt).toEpochMilli(),
        isRead
    )

fun NotificationEntity.asExternalModel(): com.ssafy.cafeminsu.core.model.notification.AppNotification =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.notification.AppNotification(
        id = id.toString(),
        type = type.asNotificationType(),
        title = title,
        body = body,
        createdAtMillis = createdAtMillis,
        read = isRead,
    )

private fun String.asNotificationType(): com.ssafy.cafeminsu.core.model.notification.NotificationType =
    when (uppercase()) {
        "ORDER_ACCEPTED" -> NotificationType.OrderAccepted
        "ORDER_READY" -> NotificationType.OrderReady
        "ORDER_COMPLETED" -> NotificationType.OrderCompleted
        "STAMP", "STAMP_EARNED" -> NotificationType.StampEarned
        "GIFTICON", "GIFTICON_RECEIVED" -> NotificationType.GifticonReceived
        else -> NotificationType.System
    }
