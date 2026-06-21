package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationApiTest {
    @Test
    fun notificationDtoKeepsOpenApiFields() {
        val notification = NotificationListItemRes(
            id = 71,
            title = "주문이 준비됐어요",
            body = "주문번호 A-2419",
            type = "ORDER",
            isRead = false,
            relatedEntityId = 2419,
            createdAt = "2026-06-20T01:20:30Z",
        )

        assertEquals(71L, notification.id)
        assertEquals("주문이 준비됐어요", notification.title)
        assertEquals("주문번호 A-2419", notification.body)
        assertEquals("ORDER", notification.type)
        assertEquals(false, notification.isRead)
        assertEquals(2419L, notification.relatedEntityId)
        assertEquals("2026-06-20T01:20:30Z", notification.createdAt)
    }
}
