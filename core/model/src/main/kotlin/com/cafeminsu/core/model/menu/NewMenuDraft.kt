package com.cafeminsu.core.model.menu

import com.cafeminsu.core.model.media.ImageSource

data class NewMenuDraft(
    val name: String,
    val categoryId: String,
    val basePrice: Int,
    val description: String,
    val image: ImageSource,
    val isSoldOut: Boolean,
)
