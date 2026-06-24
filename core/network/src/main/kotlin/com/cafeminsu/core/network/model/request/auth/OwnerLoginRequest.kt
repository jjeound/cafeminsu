package com.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class OwnerLoginRequest(val loginId: String, val password: String)
