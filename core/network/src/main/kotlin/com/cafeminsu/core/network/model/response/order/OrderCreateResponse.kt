package com.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderCreateResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "orderNumber") val orderNumber: String,
    @SerialName(value = "totalAmount") val totalAmount: Int,
    @SerialName(value = "status") val status: String,
)
