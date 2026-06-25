package com.ssafy.cafeminsu.core.data.repository.reward

import com.ssafy.cafeminsu.core.model.reward.Coupon
import kotlinx.coroutines.flow.Flow

interface CouponRepository {
    fun observeCoupons(): Flow<List<Coupon>>

    fun useCoupon(id: String): Flow<Coupon>
}
