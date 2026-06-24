package com.cafeminsu.core.model.store

import com.cafeminsu.core.model.media.ImageSource

data class NearbyStoreSummary(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val image: ImageSource,
)
