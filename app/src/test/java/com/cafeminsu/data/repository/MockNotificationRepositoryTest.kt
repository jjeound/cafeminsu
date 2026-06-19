package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.NotificationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockNotificationRepositoryTest {
    @Test
    fun observeNotificationsEmitsDesignSeedNotifications() = runBlocking {
        val repository = MockNotificationRepository(nowMillis = { SeedNowMillis })

        repository.observeNotifications().test {
            val notifications = awaitItem().successData()

            assertEquals(5, notifications.size)
            assertEquals(NotificationType.OrderReady, notifications[0].type)
            assertEquals("주문이 준비됐어요", notifications[0].title)
            assertEquals("주문번호 A-2419 — 픽업대에서 수령해주세요", notifications[0].body)
            assertEquals(NotificationType.GifticonReceived, notifications[3].type)
            assertEquals("기프티콘이 도착했어요", notifications[3].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markAllReadUpdatesUnreadNotifications() = runBlocking {
        val repository = MockNotificationRepository(nowMillis = { SeedNowMillis })

        repository.observeNotifications().test {
            assertTrue(awaitItem().successData().any { !it.read })

            repository.markAllRead()

            assertFalse(awaitItem().successData().any { !it.read })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }

    private companion object {
        const val SeedNowMillis = 1_803_981_600_000L
    }
}
