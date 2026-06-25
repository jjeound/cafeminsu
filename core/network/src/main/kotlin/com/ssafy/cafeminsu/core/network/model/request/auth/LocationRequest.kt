package com.ssafy.cafeminsu.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class LocationRequest(val latitude: Double, val longitude: Double)
