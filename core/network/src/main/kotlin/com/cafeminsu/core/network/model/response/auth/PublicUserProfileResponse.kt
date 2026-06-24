package com.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class PublicUserProfileResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
