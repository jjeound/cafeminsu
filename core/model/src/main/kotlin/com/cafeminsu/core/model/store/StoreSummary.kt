package com.cafeminsu.core.model.store

data class StoreSummary(
    val id: Long,
    val name: String,
    val address: String,
    val imageUrl: String?,
)
