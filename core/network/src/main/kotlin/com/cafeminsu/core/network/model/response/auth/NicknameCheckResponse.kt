package com.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NicknameCheckResponse(
    @SerialName(value = "available") val available: Boolean,
)
