package com.cafeminsu.core.model.reward

data class Coupon(
    val id: String,
    val title: String,
    val benefit: CouponBenefit,
    val expiresAtMillis: Long,
    val status: CouponStatus,
)
