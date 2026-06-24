package com.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuOptionResponse(
    @SerialName(value = "optionId") val optionId: Long,
    @SerialName(value = "optionGroup") val optionGroup: String,
    @SerialName(value = "optionName") val optionName: String,
    @SerialName(value = "optionPrice") val optionPrice: Int,
    @SerialName(value = "isDefault") val isDefault: Boolean,
)
