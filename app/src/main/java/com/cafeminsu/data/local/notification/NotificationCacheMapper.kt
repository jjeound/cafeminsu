package com.cafeminsu.data.local.notification

import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType

/**
 * 도메인 [AppNotification] ↔ 캐시 [NotificationEntity] 순수 매핑.
 *
 * enum(type) 은 name 문자열로 직렬화한다. 미지의 type 문자열은 기본값으로 흡수해
 * 캐시 손상이 화면 오류로 번지지 않게 한다(StoreCacheMapper 와 동일 정책).
 */
fun AppNotification.toNotificationEntity(): NotificationEntity =
    NotificationEntity(
        id = id,
        type = type.name,
        title = title,
        body = body,
        createdAtMillis = createdAtMillis,
        read = read,
    )

fun NotificationEntity.toAppNotification(): AppNotification =
    AppNotification(
        id = id,
        type = type.toNotificationType(),
        title = title,
        body = body,
        createdAtMillis = createdAtMillis,
        read = read,
    )

fun List<AppNotification>.toNotificationEntities(): List<NotificationEntity> =
    map(AppNotification::toNotificationEntity)

fun List<NotificationEntity>.toAppNotifications(): List<AppNotification> =
    map(NotificationEntity::toAppNotification)

private fun String.toNotificationType(): NotificationType =
    NotificationType.entries.firstOrNull { it.name == this } ?: NotificationType.OrderAccepted
