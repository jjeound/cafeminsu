package com.ssafy.cafeminsu.core.network.model.request.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuUpdateRequest(
    @SerialName(value = "name") val name: String? = null,
    @SerialName(value = "description") val description: String? = null,
    @SerialName(value = "price") val price: Int? = null,
    @SerialName(value = "category") val category: String? = null,
    @SerialName(value = "imageUrl") val imageUrl: String? = null,
)
