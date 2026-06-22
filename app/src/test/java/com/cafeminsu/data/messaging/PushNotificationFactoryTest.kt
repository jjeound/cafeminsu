package com.cafeminsu.data.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushNotificationFactoryTest {
    @Test
    fun usesNotificationPayloadTitleAndBody() {
        val content = PushNotificationFactory.from(
            notificationTitle = "주문이 준비됐어요",
            notificationBody = "픽업대에서 수령해주세요",
            data = emptyMap(),
        )

        requireNotNull(content)
        assertEquals("주문이 준비됐어요", content.title)
        assertEquals("픽업대에서 수령해주세요", content.body)
        assertEquals(PushNotificationFactory.CHANNEL_ID, content.channelId)
        assertEquals(PushNotificationFactory.CHANNEL_NAME, content.channelName)
    }

    @Test
    fun fallsBackToDataPayloadWhenNotificationBlockMissing() {
        val content = PushNotificationFactory.from(
            notificationTitle = null,
            notificationBody = null,
            data = mapOf(
                PushNotificationFactory.DATA_KEY_TITLE to "스탬프가 적립됐어요",
                PushNotificationFactory.DATA_KEY_BODY to "스탬프 1개 적립",
            ),
        )

        requireNotNull(content)
        assertEquals("스탬프가 적립됐어요", content.title)
        assertEquals("스탬프 1개 적립", content.body)
    }

    @Test
    fun returnsNullWhenNeitherTitleNorBodyPresent() {
        assertNull(
            PushNotificationFactory.from(
                notificationTitle = null,
                notificationBody = null,
                data = emptyMap(),
            ),
        )
        assertNull(
            PushNotificationFactory.from(
                notificationTitle = "   ",
                notificationBody = "",
                data = emptyMap(),
            ),
        )
    }

    @Test
    fun blankTitleFallsBackToDefaultButKeepsBody() {
        val content = PushNotificationFactory.from(
            notificationTitle = null,
            notificationBody = "본문만 있는 알림",
            data = emptyMap(),
        )

        requireNotNull(content)
        assertEquals("민수", content.title)
        assertEquals("본문만 있는 알림", content.body)
    }

    @Test
    fun notificationIdIsStableForSameTypeAndRelatedId() {
        val first = PushNotificationFactory.from(
            notificationTitle = "t",
            notificationBody = "b",
            data = mapOf(
                PushNotificationFactory.DATA_KEY_TYPE to "ORDER",
                PushNotificationFactory.DATA_KEY_RELATED_ID to "2419",
            ),
        )
        val second = PushNotificationFactory.from(
            notificationTitle = "t",
            notificationBody = "b",
            data = mapOf(
                PushNotificationFactory.DATA_KEY_TYPE to "ORDER",
                PushNotificationFactory.DATA_KEY_RELATED_ID to "2419",
            ),
        )
        val other = PushNotificationFactory.from(
            notificationTitle = "t",
            notificationBody = "b",
            data = mapOf(
                PushNotificationFactory.DATA_KEY_TYPE to "STAMP",
                PushNotificationFactory.DATA_KEY_RELATED_ID to "88",
            ),
        )

        assertEquals(first!!.notificationId, second!!.notificationId)
        assertNotEquals(first.notificationId, other!!.notificationId)
    }

    @Test
    fun notificationIdFallsBackToDefaultWithoutTypeOrRelatedId() {
        val content = PushNotificationFactory.from(
            notificationTitle = "t",
            notificationBody = "b",
            data = emptyMap(),
        )

        assertEquals(PushNotificationFactory.DEFAULT_NOTIFICATION_ID, content!!.notificationId)
    }
}
