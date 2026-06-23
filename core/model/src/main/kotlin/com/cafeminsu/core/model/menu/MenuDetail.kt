package com.cafeminsu.core.model.menu

import com.cafeminsu.core.model.media.ImageSource

data class MenuDetail(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val image: ImageSource,
    val isAvailable: Boolean,
    val options: List<MenuOption>,
)
