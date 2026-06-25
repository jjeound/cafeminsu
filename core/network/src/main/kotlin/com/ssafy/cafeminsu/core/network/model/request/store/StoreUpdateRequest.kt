package com.ssafy.cafeminsu.core.network.model.request.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreUpdateRequest(
    @SerialName(value = "name") val name: String? = null,
    @SerialName(value = "address") val address: String? = null,
    @SerialName(value = "phone") val phone: String? = null,
    @SerialName(value = "businessHours") val businessHours: String? = null,
    @SerialName(value = "imageUrl") val imageUrl: String? = null,
)
