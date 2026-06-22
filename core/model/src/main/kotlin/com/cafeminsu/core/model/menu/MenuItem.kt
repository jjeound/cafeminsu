package com.cafeminsu.core.model.menu

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val options: List<MenuOptionGroup>,
    val isVisible: Boolean = true,
)
