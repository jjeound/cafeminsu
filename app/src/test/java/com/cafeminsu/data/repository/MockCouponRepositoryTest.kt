package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.CouponStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCouponRepositoryTest {
    @Test
    fun observesSeededCouponsAndMarksCouponUsed() = runBlocking {
        val repository = MockCouponRepository()
        val initial = repository.observeCoupons().first()
        assertTrue(initial is AppResult.Success)

        val coupon = (initial as AppResult.Success).data.first()
        val used = repository.useCoupon(coupon.id)

        assertTrue(used is AppResult.Success)
        assertEquals(CouponStatus.Used, (used as AppResult.Success).data.status)
    }
}
