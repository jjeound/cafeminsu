package com.cafeminsu.core.model.store

data class NearbyStoreSummary(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val image: String,
)
