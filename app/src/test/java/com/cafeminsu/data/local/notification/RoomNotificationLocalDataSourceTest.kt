package com.cafeminsu.data.local.notification

import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomNotificationLocalDataSourceTest {
    private val dao = mockk<NotificationDao>(relaxed = true)
    private val dataSource = RoomNotificationLocalDataSource(dao)

    @Test
    fun cachedNotificationsMapsDaoEntitiesToDomain() = runTest {
        val notification = sampleNotification()
        coEvery { dao.getAll() } returns listOf(notification.toNotificationEntity())

        assertEquals(listOf(notification), dataSource.cachedNotifications())
    }

    @Test
    fun replaceNotificationsClearsThenUpsertsMappedEntities() = runTest {
        val captured = slot<List<NotificationEntity>>()
        coEvery { dao.upsertAll(capture(captured)) } returns Unit

        dataSource.replaceNotifications(listOf(sampleNotification()))

        // 사라진 알림이 남지 않도록 전체를 비운 뒤 다시 채우는 순서를 보장한다.
        coVerifyOrder {
            dao.clear()
            dao.upsertAll(any())
        }
        assertEquals("71", captured.captured.single().id)
        assertEquals("OrderReady", captured.captured.single().type)
    }

    private fun sampleNotification(): AppNotification =
        AppNotification(
            id = "71",
            type = NotificationType.OrderReady,
            title = "주문이 준비됐어요",
            body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
            createdAtMillis = 1_700_000_000_000L,
            read = false,
        )
}
