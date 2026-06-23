package com.cafeminsu.core.model.menu

import com.cafeminsu.core.model.media.ImageSource

data class MenuSummary(
    val id: Long,
    val name: String,
    val price: Int,
    val category: String,
    val image: ImageSource,
    val isAvailable: Boolean,
)
