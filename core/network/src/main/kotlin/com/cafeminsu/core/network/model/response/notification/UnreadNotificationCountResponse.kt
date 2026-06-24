package com.cafeminsu.core.network.model.response.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnreadNotificationCountResponse(
    @SerialName(value = "count") val count: Int,
)
