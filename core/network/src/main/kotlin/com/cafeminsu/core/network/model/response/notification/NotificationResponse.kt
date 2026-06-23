package com.cafeminsu.core.network.model.response.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "title") val title: String,
    @SerialName(value = "body") val body: String,
    @SerialName(value = "type") val type: String,
    @SerialName(value = "isRead") val isRead: Boolean,
    @SerialName(value = "relatedEntityId") val relatedEntityId: Long?,
    @SerialName(value = "createdAt") val createdAt: String,
)
