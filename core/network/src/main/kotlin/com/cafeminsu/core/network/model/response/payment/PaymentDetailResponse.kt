package com.cafeminsu.core.network.model.response.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDetailResponse(
    @SerialName(value = "paymentId") val paymentId: Long,
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "method") val method: String,
    @SerialName(value = "amount") val amount: Int,
    @SerialName(value = "status") val status: String,
    @SerialName(value = "paidAt") val paidAt: String,
)
