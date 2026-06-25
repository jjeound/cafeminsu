package com.cafeminsu.core.model.menu

data class MenuSummary(
    val id: Long,
    val name: String,
    val price: Int,
    val category: String,
    val image: String,
    val isAvailable: Boolean,
)
