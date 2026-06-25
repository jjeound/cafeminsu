package com.ssafy.cafeminsu.core.network.model.response.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyStoreResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "distance") val distance: Double,
    @SerialName(value = "imageUrl") val imageUrl: String?,
)
