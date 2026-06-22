package com.cafeminsu.core.data.repository.notification

import com.cafeminsu.core.model.notification.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<AppNotification>>

    fun markAllRead(): Flow<Unit>
}
