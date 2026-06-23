package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonUseResponse(
    @SerialName(value = "balanceAfter") val balanceAfter: Int,
    @SerialName(value = "status") val status: String,
)
