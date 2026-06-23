package com.cafeminsu.core.network.model.response.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentPrepareResponse(
    @SerialName(value = "merchantUid") val merchantUid: String,
    @SerialName(value = "amount") val amount: Int,
)
