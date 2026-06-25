package com.ssafy.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuListItemResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "price") val price: Int,
    @SerialName(value = "category") val category: String,
    @SerialName(value = "imageUrl") val imageUrl: String?,
    @SerialName(value = "isAvailable") val isAvailable: Boolean,
)
