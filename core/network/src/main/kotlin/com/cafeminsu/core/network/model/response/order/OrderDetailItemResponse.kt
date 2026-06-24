package com.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDetailItemResponse(
    @SerialName(value = "menuId") val menuId: Long,
    @SerialName(value = "menuName") val menuName: String,
    @SerialName(value = "quantity") val quantity: Int,
    @SerialName(value = "unitPrice") val unitPrice: Int,
    @SerialName(value = "options") val options: List<OrderDetailOptionResponse>,
    @SerialName(value = "subtotal") val subtotal: Int,
)
