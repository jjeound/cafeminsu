package com.cafeminsu.core.network.model.request.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuOptionUpdateRequest(
    @SerialName(value = "optionGroup") val optionGroup: String? = null,
    @SerialName(value = "optionName") val optionName: String? = null,
    @SerialName(value = "additionalPrice") val additionalPrice: Int? = null,
    @SerialName(value = "isDefault") val isDefault: Boolean? = null,
)
