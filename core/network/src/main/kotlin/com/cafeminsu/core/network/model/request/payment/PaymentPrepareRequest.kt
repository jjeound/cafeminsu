package com.cafeminsu.core.network.model.request.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentPrepareRequest(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "useGifticonId") val useGifticonId: Long? = null,
    @SerialName(value = "gifticonAmount") val gifticonAmount: Int? = null,
    @SerialName(value = "cardAmount") val cardAmount: Int? = null,
)
