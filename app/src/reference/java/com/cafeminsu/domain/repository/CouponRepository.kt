package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

interface CouponRepository {
    fun observeCoupons(): Flow<AppResult<List<Coupon>>>
    suspend fun useCoupon(id: String): AppResult<Coupon>
}
