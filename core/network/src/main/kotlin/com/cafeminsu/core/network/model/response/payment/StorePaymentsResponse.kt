package com.cafeminsu.core.network.model.response.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorePaymentsResponse(
    @SerialName(value = "total") val total: Int,
    @SerialName(value = "payments") val payments: List<StorePaymentLineResponse>,
)

@Serializable
data class StorePaymentLineResponse(
    @SerialName(value = "menuId") val menuId: Long,
    @SerialName(value = "quantity") val quantity: Int,
    @SerialName(value = "optionIds") val optionIds: List<Long>,
)
