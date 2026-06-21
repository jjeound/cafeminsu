package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.remote.NotificationListItemRes
import com.cafeminsu.domain.model.NotificationType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {
    @Test
    fun notificationListMapsTypesReadStateEpochMillisAndSortsByCreatedAtDescending() {
        val result = listOf(
            notification(
                id = 71,
                title = "주문이 준비됐어요",
                body = "주문번호 A-2419",
                type = "ORDER",
                isRead = false,
                createdAt = "2026-06-20T01:20:30Z",
            ),
            notification(
                id = 72,
                title = "스탬프가 적립됐어요",
                type = "STAMP",
                isRead = true,
                createdAt = "2026-06-20T01:15:30Z",
            ),
            notification(
                id = 73,
                title = "기프티콘이 도착했어요",
                type = "GIFTICON",
                createdAt = "2026-06-19T01:20:30Z",
            ),
            notification(
                id = 74,
                title = "주문이 완료됐어요",
                type = "ORDER",
                createdAt = "2026-06-18T01:20:30Z",
            ),
            notification(
                id = 75,
                title = "새 소식",
                type = "SYSTEM",
                createdAt = "2026-06-21T01:20:30Z",
            ),
        ).toAppNotifications()

        assertTrue(result is AppResult.Success)
        val notifications = (result as AppResult.Success).data
        assertEquals(listOf("71", "72", "73", "74"), notifications.map { it.id })
        assertEquals(
            listOf(
                NotificationType.OrderReady,
                NotificationType.StampEarned,
                NotificationType.GifticonReceived,
                NotificationType.OrderCompleted,
            ),
            notifications.map { it.type },
        )
        assertEquals(false, notifications[0].read)
        assertEquals(true, notifications[1].read)
        assertEquals(Instant.parse("2026-06-20T01:20:30Z").toEpochMilli(), notifications[0].createdAtMillis)
    }

    @Test
    fun orderNotificationFallsBackToAcceptedWhenServerTypeIsCoarse() {
        val result = listOf(
            notification(
                id = 81,
                title = "주문이 접수됐어요",
                type = "ORDER",
            ),
        ).toAppNotifications()

        assertTrue(result is AppResult.Success)
        assertEquals(NotificationType.OrderAccepted, (result as AppResult.Success).data.single().type)
    }

    private fun notification(
        id: Long,
        title: String,
        body: String = "알림 내용",
        type: String,
        isRead: Boolean = false,
        relatedEntityId: Long? = null,
        createdAt: String = "2026-06-20T01:20:30Z",
    ): NotificationListItemRes =
        NotificationListItemRes(
            id = id,
            title = title,
            body = body,
            type = type,
            isRead = isRead,
            relatedEntityId = relatedEntityId,
            createdAt = createdAt,
        )
}
