package com.cafeminsu.core.model.menu

data class MenuOption(
    val id: Long,
    val groupName: String,
    val name: String,
    val additionalPrice: Int,
    val isDefault: Boolean,
)
