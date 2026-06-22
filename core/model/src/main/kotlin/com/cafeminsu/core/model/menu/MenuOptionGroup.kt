package com.cafeminsu.core.model.menu

data class MenuOptionGroup(
    val id: String,
    val name: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<MenuOption>,
)
