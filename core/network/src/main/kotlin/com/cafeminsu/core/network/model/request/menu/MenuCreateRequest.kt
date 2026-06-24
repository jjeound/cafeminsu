package com.cafeminsu.core.network.model.request.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuCreateRequest(
    @SerialName(value = "name") val name: String,
    @SerialName(value = "description") val description: String,
    @SerialName(value = "price") val price: Int,
    @SerialName(value = "category") val category: String,
    @SerialName(value = "imageUrl") val imageUrl: String? = null,
    @SerialName(value = "isAvailable") val isAvailable: Boolean,
    @SerialName(value = "options") val options: List<MenuOptionRequest>,
)
