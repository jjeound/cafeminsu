package com.cafeminsu.core.network.model.request.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItemRequest(
    @SerialName(value = "menuId") val menuId: Long,
    @SerialName(value = "quantity") val quantity: Int,
    @SerialName(value = "optionIds") val optionIds: List<Long>?,
)
