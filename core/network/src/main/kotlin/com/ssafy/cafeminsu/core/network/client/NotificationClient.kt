package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.response.notification.NotificationResponse
import com.ssafy.cafeminsu.core.network.model.response.notification.UnreadNotificationCountResponse
import com.ssafy.cafeminsu.core.network.service.NotificationService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class NotificationClient @Inject constructor(private val notificationService: NotificationService) {
    suspend fun getNotifications(
        isRead: Boolean? = null,
        limit: Int = 20,
    ): List<NotificationResponse> = notificationService.getNotifications(isRead, limit)

    suspend fun getUnreadCount(): UnreadNotificationCountResponse = notificationService.getUnreadCount()

    suspend fun markRead(id: Long) {
        notificationService.markRead(id)
    }

    suspend fun markAllRead() {
        notificationService.markAllRead()
    }
}
