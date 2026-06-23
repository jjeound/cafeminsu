package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.reward.Stamp

@Serializable
data class StampSummaryResponse(
    @SerialName(value = "storeId") val storeId: Long,
    @SerialName(value = "storeName") val storeName: String,
    @SerialName(value = "count") val count: Int,
)

fun StampSummaryResponse.asExternalModel(): Stamp =
    Stamp(storeId = storeId, storeName = storeName, count = count, histories = emptyList())
