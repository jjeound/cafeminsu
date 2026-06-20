package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Coupon
import com.cafeminsu.domain.model.CouponStatus
import com.cafeminsu.domain.model.CouponType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class CouponRepositoryTest {
    @Test
    fun exposesCouponRepositoryContract() = runBlocking {
        val repository = object : CouponRepository {
            override fun observeCoupons(): Flow<AppResult<List<Coupon>>> =
                flowOf(AppResult.Success(listOf(sampleCoupon())))

            override suspend fun useCoupon(id: String): AppResult<Coupon> =
                AppResult.Failure(DomainError.NotFound)
        }

        assertNotNull(repository.observeCoupons())
    }

    private fun sampleCoupon(): Coupon =
        Coupon(
            id = "coupon-1",
            type = CouponType.FreeDrink,
            title = "무료 음료 1잔 쿠폰",
            amount = null,
            expiresAtMillis = 1_809_331_200_000L,
            status = CouponStatus.Available,
        )
}
