package com.ssafy.cafeminsu.core.network.model.request.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuOptionRequest(
    @SerialName(value = "optionGroup") val optionGroup: String,
    @SerialName(value = "optionName") val optionName: String,
    @SerialName(value = "additionalPrice") val additionalPrice: Int,
    @SerialName(value = "isDefault") val isDefault: Boolean,
)
