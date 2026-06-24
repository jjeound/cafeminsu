package com.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenRequest(val fcmToken: String)
