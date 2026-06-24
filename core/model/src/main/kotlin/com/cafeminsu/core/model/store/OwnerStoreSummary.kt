package com.cafeminsu.core.model.store

import com.cafeminsu.core.model.media.ImageSource

data class OwnerStoreSummary(
    val id: Long,
    val name: String,
    val image: ImageSource,
)
