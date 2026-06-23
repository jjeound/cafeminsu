package com.cafeminsu.core.model.reward

sealed interface CouponBenefit {
    data object FreeDrink : CouponBenefit

    data class Amount(val value: Int) : CouponBenefit
}
