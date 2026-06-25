package com.ssafy.cafeminsu.core.network.model.request.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonUseRequest(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "usedAmount") val usedAmount: Int,
)
