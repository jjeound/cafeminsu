package com.ssafy.cafeminsu.core.network.model.request.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonPurchaseRequest(
    @SerialName(value = "amount") val amount: Int,
    @SerialName(value = "receiverId") val receiverId: Long? = null,
    @SerialName(value = "receiverPhone") val receiverPhone: String? = null,
    @SerialName(value = "message") val message: String? = null,
)
