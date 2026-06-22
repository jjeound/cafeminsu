package com.cafeminsu.data.local.notification

import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCacheMapperTest {
    @Test
    fun roundTripPreservesAllFields() {
        val notification = AppNotification(
            id = "71",
            type = NotificationType.OrderReady,
            title = "주문이 준비됐어요",
            body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
            createdAtMillis = 1_700_000_000_000L,
            read = false,
        )

        val restored = notification.toNotificationEntity().toAppNotification()

        assertEquals(notification, restored)
    }

    @Test
    fun listRoundTripPreservesOrderAndTypes() {
        val notifications = listOf(
            AppNotification(
                id = "71",
                type = NotificationType.OrderReady,
                title = "주문 준비",
                body = "",
                createdAtMillis = 30L,
                read = false,
            ),
            AppNotification(
                id = "72",
                type = NotificationType.StampEarned,
                title = "스탬프 적립",
                body = "",
                createdAtMillis = 20L,
                read = true,
            ),
            AppNotification(
                id = "73",
                type = NotificationType.GifticonReceived,
                title = "기프티콘 도착",
                body = "",
                createdAtMillis = 10L,
                read = false,
            ),
        )

        val restored = notifications.toNotificationEntities().toAppNotifications()

        assertEquals(notifications, restored)
    }

    @Test
    fun typeIsStoredAsEnumNameOnEntity() {
        val notification = AppNotification(
            id = "73",
            type = NotificationType.GifticonReceived,
            title = "기프티콘이 도착했어요",
            body = "선물함에서 확인해주세요",
            createdAtMillis = 5L,
            read = true,
        )

        val entity = notification.toNotificationEntity()

        assertEquals("GifticonReceived", entity.type)
        assertEquals(NotificationType.GifticonReceived, entity.toAppNotification().type)
    }

    @Test
    fun unknownTypeStringDecodesToFallbackWithoutCrash() {
        // 캐시 type 문자열이 손상돼도 화면 오류로 번지지 않도록 기본값으로 흡수한다.
        val entity = NotificationEntity(
            id = "99",
            type = "UNKNOWN_TYPE",
            title = "알 수 없음",
            body = "",
            createdAtMillis = 1L,
            read = false,
        )

        assertEquals(NotificationType.OrderAccepted, entity.toAppNotification().type)
    }
}
