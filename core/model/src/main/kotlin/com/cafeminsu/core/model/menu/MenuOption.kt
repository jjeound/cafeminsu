package com.cafeminsu.core.model.menu

data class MenuOption(
    val id: String,
    val name: String,
    val extraPrice: Int,
    val isAvailable: Boolean,
)
