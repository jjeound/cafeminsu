package com.ssafy.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonPurchaseResponse(
    @SerialName(value = "gifticonId") val gifticonId: Long,
    @SerialName(value = "qrCode") val qrCode: String,
    @SerialName(value = "merchantUid") val merchantUid: String,
)
