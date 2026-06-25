package com.ssafy.cafeminsu.core.network.model.request.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentVerifyRequest(
    @SerialName(value = "impUid") val impUid: String,
    @SerialName(value = "merchantUid") val merchantUid: String,
)
