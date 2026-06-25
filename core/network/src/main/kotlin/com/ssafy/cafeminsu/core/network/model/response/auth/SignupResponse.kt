package com.ssafy.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupResponse(
    @SerialName(value = "userId") val userId: Long,
    @SerialName(value = "nickname") val nickname: String,
)
