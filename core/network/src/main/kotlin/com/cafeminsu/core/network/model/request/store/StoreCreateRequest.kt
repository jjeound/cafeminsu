package com.cafeminsu.core.network.model.request.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreCreateRequest(
    @SerialName(value = "name") val name: String,
    @SerialName(value = "address") val address: String,
    @SerialName(value = "latitude") val latitude: Double,
    @SerialName(value = "longitude") val longitude: Double,
    @SerialName(value = "phone") val phone: String,
    @SerialName(value = "businessHours") val businessHours: String,
    @SerialName(value = "imageUrl") val imageUrl: String? = null,
)
