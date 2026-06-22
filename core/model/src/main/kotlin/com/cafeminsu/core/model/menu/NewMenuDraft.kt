package com.cafeminsu.core.model.menu

data class NewMenuDraft(
    val name: String,
    val categoryId: String,
    val basePrice: Int,
    val description: String,
    val imageUrl: String?,
    val isSoldOut: Boolean,
)
