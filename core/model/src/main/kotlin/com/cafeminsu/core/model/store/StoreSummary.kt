package com.cafeminsu.core.model.store

import com.cafeminsu.core.model.media.ImageSource

data class StoreSummary(
    val id: Long,
    val name: String,
    val address: String,
    val image: ImageSource,
)
