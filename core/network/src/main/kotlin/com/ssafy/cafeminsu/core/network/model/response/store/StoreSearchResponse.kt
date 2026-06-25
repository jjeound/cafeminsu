package com.ssafy.cafeminsu.core.network.model.response.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreSearchResponse(
    @SerialName(value = "stores") val stores: List<StoreSearchItemResponse>,
    @SerialName(value = "total") val total: Long,
)
