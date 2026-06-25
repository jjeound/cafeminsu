package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Coupon
import com.cafeminsu.domain.model.CouponStatus
import com.cafeminsu.domain.repository.CouponRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockCouponRepository @Inject constructor() : CouponRepository {
    private val coupons = MutableStateFlow(MockData.initialCoupons)

    override fun observeCoupons(): Flow<AppResult<List<Coupon>>> =
        coupons.map { AppResult.Success(it) }

    override suspend fun useCoupon(id: String): AppResult<Coupon> {
        if (id.isBlank()) {
            return AppResult.Failure(DomainError.Validation("id"))
        }

        val currentCoupons = coupons.value
        val coupon = currentCoupons.firstOrNull { it.id == id }
            ?: return AppResult.Failure(DomainError.NotFound)

        if (coupon.status != CouponStatus.Available) {
            return AppResult.Failure(DomainError.Validation("status"))
        }

        val used = coupon.copy(status = CouponStatus.Used)
        coupons.value = currentCoupons.map { current ->
            if (current.id == id) {
                used
            } else {
                current
            }
        }
        return AppResult.Success(used)
    }
}
