package com.cafeminsu.core.model.reward

data class StampCard(
    val userId: String,
    val currentCount: Int,
    val goalCount: Int,
    val history: List<StampEvent>,
)
