package com.ssafy.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class NicknameUpdateResponse(val nickname: String)
