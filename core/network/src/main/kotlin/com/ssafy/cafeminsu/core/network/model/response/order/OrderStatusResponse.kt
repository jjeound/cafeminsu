package com.ssafy.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderStatusResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "status") val status: String,
)
