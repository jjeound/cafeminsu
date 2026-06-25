package com.ssafy.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDetailOptionResponse(
    @SerialName(value = "optionId") val optionId: Long,
    @SerialName(value = "optionGroup") val optionGroup: String,
    @SerialName(value = "optionName") val optionName: String,
    @SerialName(value = "optionPrice") val optionPrice: Int,
)
