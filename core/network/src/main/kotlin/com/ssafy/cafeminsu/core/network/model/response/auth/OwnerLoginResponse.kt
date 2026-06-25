package com.ssafy.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class OwnerLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val nickname: String,
)
