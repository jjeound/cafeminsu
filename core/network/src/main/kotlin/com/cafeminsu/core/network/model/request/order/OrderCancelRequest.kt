package com.cafeminsu.core.network.model.request.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderCancelRequest(
    @SerialName(value = "reason") val reason: String? = null,
)
