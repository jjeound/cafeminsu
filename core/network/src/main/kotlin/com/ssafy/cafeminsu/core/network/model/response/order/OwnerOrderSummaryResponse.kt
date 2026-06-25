package com.ssafy.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwnerOrderSummaryResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "orderNumber") val orderNumber: String,
    @SerialName(value = "status") val status: String,
    @SerialName(value = "totalAmount") val totalAmount: Int,
    @SerialName(value = "items") val items: List<OwnerOrderItemSummaryResponse>,
    @SerialName(value = "createdAt") val createdAt: String,
)
