package com.cafeminsu.core.data.repository.notification

import com.cafeminsu.core.model.notification.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(isRead: Boolean? = null, limit: Int = 20): Flow<List<AppNotification>>

    fun getUnreadCount(): Flow<Int>

    fun markRead(id: Long): Flow<Unit>

    fun markAllRead(): Flow<Unit>
}
