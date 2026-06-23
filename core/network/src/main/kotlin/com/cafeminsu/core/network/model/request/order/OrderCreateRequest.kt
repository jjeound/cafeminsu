package com.cafeminsu.core.network.model.request.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderCreateRequest(
    @SerialName(value = "storeId") val storeId: Long,
    @SerialName(value = "orderType") val orderType: String,
    @SerialName(value = "orderMethod") val orderMethod: String,
    @SerialName(value = "items") val items: List<OrderItemRequest>,
)
