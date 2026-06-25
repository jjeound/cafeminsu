package com.ssafy.cafeminsu.core.network.model.response.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreSearchItemResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "address") val address: String,
    @SerialName(value = "imageUrl") val imageUrl: String?,
)
