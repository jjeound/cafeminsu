package com.cafeminsu.core.model.menu

data class MenuInput(
    val name: String,
    val categoryId: String,
    val basePrice: Int,
    val description: String,
    val image: String,
    val isSoldOut: Boolean,
)
