package com.ssafy.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderSummaryResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "orderNumber") val orderNumber: String,
    @SerialName(value = "storeName") val storeName: String,
    @SerialName(value = "totalAmount") val totalAmount: Int,
    @SerialName(value = "status") val status: String,
    @SerialName(value = "createdAt") val createdAt: String,
)
