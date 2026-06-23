package com.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    @SerialName(value = "accessToken") val accessToken: String,
)
