package com.ssafy.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginResponse(
    @SerialName(value = "accessToken") val accessToken: String,
    @SerialName(value = "refreshToken") val refreshToken: String,
    @SerialName(value = "isNewUser") val isNewUser: Boolean,
    @SerialName(value = "nickname") val nickname: String,
)
