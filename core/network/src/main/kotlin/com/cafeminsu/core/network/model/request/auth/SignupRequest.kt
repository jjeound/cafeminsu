package com.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    @SerialName(value = "nickname") val nickname: String,
    @SerialName(value = "profileImageUrl") val profileImageUrl: String? = null,
)
