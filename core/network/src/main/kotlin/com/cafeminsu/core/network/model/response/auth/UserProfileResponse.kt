package com.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "nickname") val nickname: String,
    @SerialName(value = "profileImageUrl") val profileImageUrl: String?,
    @SerialName(value = "role") val role: String,
)
