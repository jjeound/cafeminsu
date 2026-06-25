package com.ssafy.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StampSummaryResponse(
    @SerialName(value = "storeId") val storeId: Long,
    @SerialName(value = "storeName") val storeName: String,
    @SerialName(value = "count") val count: Int,
)
