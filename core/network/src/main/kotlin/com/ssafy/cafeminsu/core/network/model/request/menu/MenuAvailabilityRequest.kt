package com.ssafy.cafeminsu.core.network.model.request.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuAvailabilityRequest(
    @SerialName(value = "isAvailable") val isAvailable: Boolean,
)
