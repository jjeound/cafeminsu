package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.reward.Stamp

@Serializable
data class StampDetailResponse(
    @SerialName(value = "storeId") val storeId: Long,
    @SerialName(value = "storeName") val storeName: String,
    @SerialName(value = "count") val count: Int,
    @SerialName(value = "histories") val histories: List<StampHistoryResponse>,
)

fun StampDetailResponse.asExternalModel(): Stamp =
    Stamp(
        storeId = storeId,
        storeName = storeName,
        count = count,
        histories = histories.map(StampHistoryResponse::asExternalModel),
    )
