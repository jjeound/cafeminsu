package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.notification.NotificationResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.PATCH

interface NotificationService {
    @GET("api/notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationResponse>>

    @PATCH("api/notifications/read-all")
    suspend fun markAllRead(): ApiResponse<Unit>
}
