package com.ssafy.cafeminsu.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class LocationResponse(val latitude: Double, val longitude: Double)
