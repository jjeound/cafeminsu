package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.NotificationListItemRes
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType
import java.util.Locale

fun List<NotificationListItemRes>.toAppNotifications(): AppResult<List<AppNotification>> {
    val mapped = mapNotNull { item ->
        when (val result = item.toAppNotificationOrNull()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }.sortedByDescending { notification -> notification.createdAtMillis }

    return AppResult.Success(mapped)
}

private fun NotificationListItemRes.toAppNotificationOrNull(): AppResult<AppNotification?> {
    val notificationType = toNotificationType() ?: return AppResult.Success(null)
    val notificationId = id ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        AppNotification(
            id = notificationId.toString(),
            type = notificationType,
            title = title.orEmpty(),
            body = body.orEmpty(),
            createdAtMillis = createdAt.toEpochMillisOrZero(),
            read = isRead ?: false,
        ),
    )
}

private fun NotificationListItemRes.toNotificationType(): NotificationType? {
    val normalizedType = type.orEmpty().uppercase(Locale.US)
    val searchableText = "${title.orEmpty()} ${body.orEmpty()}".lowercase(Locale.KOREA)

    return when (normalizedType) {
        "ORDER_READY" -> NotificationType.OrderReady
        "ORDER_COMPLETED" -> NotificationType.OrderCompleted
        "ORDER_ACCEPTED" -> NotificationType.OrderAccepted
        "ORDER" -> searchableText.toOrderNotificationType()
        "STAMP",
        "STAMP_EARNED",
        -> NotificationType.StampEarned

        "GIFTICON",
        "GIFTICON_RECEIVED",
        -> NotificationType.GifticonReceived

        else -> null
    }
}

private fun String.toOrderNotificationType(): NotificationType =
    when {
        contains("ready") || contains("준비") || contains("픽업") -> NotificationType.OrderReady
        contains("completed") || contains("done") || contains("완료") -> NotificationType.OrderCompleted
        contains("accepted") || contains("접수") || contains("수락") -> NotificationType.OrderAccepted
        else -> NotificationType.OrderAccepted
    }

private fun String?.toEpochMillisOrZero(): Long = parseServerEpochMillis(this)
