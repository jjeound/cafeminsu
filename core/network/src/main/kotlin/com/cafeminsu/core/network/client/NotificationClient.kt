package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.response.notification.NotificationResponse
import com.cafeminsu.core.network.service.NotificationService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class NotificationClient @Inject constructor(private val notificationService: NotificationService) {
    suspend fun getNotifications(): List<NotificationResponse> = notificationService.getNotifications()
    suspend fun markAllRead(): Unit = notificationService.markAllRead()
}
