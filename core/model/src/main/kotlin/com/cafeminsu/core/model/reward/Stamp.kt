package com.cafeminsu.core.model.reward

data class Stamp(
    val storeId: Long,
    val storeName: String,
    val count: Int,
    val histories: List<StampHistory>,
)
