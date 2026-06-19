package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppNotificationTest {
    @Test
    fun notificationModelKeepsDisplayDataAndReadState() {
        val notification = AppNotification(
            id = "noti-1",
            type = NotificationType.OrderReady,
            title = "주문이 준비됐어요",
            body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
            createdAtMillis = 1_803_981_600_000L,
            read = false,
        )

        assertEquals("noti-1", notification.id)
        assertEquals(NotificationType.OrderReady, notification.type)
        assertEquals("주문이 준비됐어요", notification.title)
        assertFalse(notification.read)
    }
}
