package com.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class NicknameUpdateRequest(val nickname: String)
