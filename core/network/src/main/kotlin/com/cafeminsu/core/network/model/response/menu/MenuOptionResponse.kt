package com.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.menu.MenuOption

@Serializable
data class MenuOptionResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "group") val group: String,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "additionalPrice") val additionalPrice: Int,
    @SerialName(value = "isDefault") val isDefault: Boolean,
)

fun MenuOptionResponse.asExternalModel(): MenuOption =
    MenuOption(
        id = id,
        groupName = group,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )
