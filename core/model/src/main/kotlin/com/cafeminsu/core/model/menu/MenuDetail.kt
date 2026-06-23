package com.cafeminsu.core.model.menu

data class MenuDetail(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val options: List<MenuOption>,
)
