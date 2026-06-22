package com.cafeminsu.core.model.reward

data class Coupon(
    val id: String,
    val type: CouponType,
    val title: String,
    val amount: Int?,
    val expiresAtMillis: Long,
    val status: CouponStatus,
)
