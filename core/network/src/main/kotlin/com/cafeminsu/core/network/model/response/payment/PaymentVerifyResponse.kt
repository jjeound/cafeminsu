package com.cafeminsu.core.network.model.response.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentVerifyResponse(
    @SerialName(value = "paymentId") val paymentId: Long,
    @SerialName(value = "status") val status: String,
)
