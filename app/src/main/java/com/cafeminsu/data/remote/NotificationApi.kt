package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.PATCH

interface NotificationApi {
    @GET("api/notifications")
    suspend fun getNotifications(): BaseResponse<List<NotificationListItemRes>>

    @PATCH("api/notifications/read-all")
    suspend fun markAllRead(): BaseResponse<NotificationReadAllRes>
}

@JsonClass(generateAdapter = true)
data class NotificationListItemRes(
    val id: Long?,
    val title: String?,
    val body: String?,
    val type: String?,
    val isRead: Boolean?,
    val relatedEntityId: Long?,
    val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class NotificationReadAllRes(
    val ignored: String? = null,
)
