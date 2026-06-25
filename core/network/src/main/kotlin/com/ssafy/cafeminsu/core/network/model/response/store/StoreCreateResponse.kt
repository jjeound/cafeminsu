package com.ssafy.cafeminsu.core.network.model.response.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreCreateResponse(
    @SerialName(value = "storeId") val storeId: Long,
)
