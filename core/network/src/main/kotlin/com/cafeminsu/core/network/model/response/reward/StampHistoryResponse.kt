package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StampHistoryResponse(
    @SerialName(value = "earnedCount") val earnedCount: Int,
    @SerialName(value = "createdAt") val createdAt: String,
)
