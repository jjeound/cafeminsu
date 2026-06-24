package com.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuCreateResponse(
    @SerialName(value = "menuId") val menuId: Long,
)
