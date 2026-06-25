package com.ssafy.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwnerOrderItemSummaryResponse(
    @SerialName(value = "menuId") val menuId: Long,
    @SerialName(value = "menuName") val menuName: String,
    @SerialName(value = "quantity") val quantity: Int,
)
