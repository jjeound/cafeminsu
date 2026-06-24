package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.notification.NotificationResponse
import com.cafeminsu.core.network.model.response.notification.UnreadNotificationCountResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationService {
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("isRead") isRead: Boolean? = null,
        @Query("limit") limit: Int = 20,
    ): List<NotificationResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): UnreadNotificationCountResponse

    @PATCH("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long)

    @PATCH("api/notifications/read-all")
    suspend fun markAllRead(): Unit
}
