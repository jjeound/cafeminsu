package com.cafeminsu.core.model.store

import com.cafeminsu.core.model.media.ImageSource

data class StoreDetail(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val businessHours: String,
    val image: ImageSource,
)
