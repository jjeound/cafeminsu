package com.cafeminsu.domain.model

data class MenuCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val options: List<MenuOptionGroup>,
)

data class MenuOptionGroup(
    val id: String,
    val name: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<MenuOption>,
)

data class MenuOption(
    val id: String,
    val name: String,
    val extraPrice: Int,
    val isAvailable: Boolean,
)
