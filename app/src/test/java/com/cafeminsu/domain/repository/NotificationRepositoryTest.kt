package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRepositoryTest {
    @Test
    fun notificationRepositoryContractExposesObservationAndReadMutation() = runBlocking {
        val repository = ContractFakeNotificationRepository(
            listOf(
                AppNotification(
                    id = "noti-1",
                    type = NotificationType.OrderAccepted,
                    title = "주문이 수락됐어요",
                    body = "주문번호 A-2419",
                    createdAtMillis = 1_803_981_600_000L,
                    read = false,
                ),
            ),
        )

        val result = repository.markAllRead()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(true, repository.markAllReadCalled)
    }
}

private class ContractFakeNotificationRepository(
    initialNotifications: List<AppNotification>,
) : NotificationRepository {
    private val notifications = MutableStateFlow(AppResult.Success(initialNotifications))
    var markAllReadCalled: Boolean = false
        private set

    override fun observeNotifications(): Flow<AppResult<List<AppNotification>>> = notifications

    override suspend fun markAllRead(): AppResult<Unit> {
        markAllReadCalled = true
        return AppResult.Success(Unit)
    }
}
