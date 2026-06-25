package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockNotificationRepository(
    private val nowMillis: () -> Long,
) : NotificationRepository {
    @Inject
    constructor() : this(nowMillis = { System.currentTimeMillis() })

    private val notifications = MutableStateFlow(MockData.initialNotifications(nowMillis()))

    override fun observeNotifications(): Flow<AppResult<List<AppNotification>>> =
        notifications.map { AppResult.Success(it) }

    override suspend fun markAllRead(): AppResult<Unit> {
        notifications.value = notifications.value.map { notification ->
            notification.copy(read = true)
        }
        return AppResult.Success(Unit)
    }
}
