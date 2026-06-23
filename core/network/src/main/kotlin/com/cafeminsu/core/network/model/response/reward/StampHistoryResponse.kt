package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.reward.StampHistory

@Serializable
data class StampHistoryResponse(
    @SerialName(value = "earnedCount") val earnedCount: Int,
    @SerialName(value = "createdAt") val createdAt: String,
)

fun StampHistoryResponse.asExternalModel(): StampHistory =
    StampHistory(earnedCount = earnedCount, createdAt = createdAt)
