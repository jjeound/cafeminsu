package com.ssafy.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonResponse(
    @SerialName(value = "gifticonId") val gifticonId: Long,
    @SerialName(value = "amount") val amount: Int,
    @SerialName(value = "balance") val balance: Int,
    @SerialName(value = "qrCode") val qrCode: String,
    @SerialName(value = "status") val status: String,
    @SerialName(value = "expiresAt") val expiresAt: String,
    @SerialName(value = "message") val message: String?,
)
