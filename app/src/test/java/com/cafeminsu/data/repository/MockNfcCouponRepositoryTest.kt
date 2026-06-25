package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockNfcCouponRepositoryTest {
    private val repository = MockNfcCouponRepository()

    @Test
    fun claimReturnsDummyCoupon() = runTest {
        val result = repository.claim("NFC-AB7K-9QM2")

        assertTrue(result is AppResult.Success)
        val coupon = (result as AppResult.Success).data
        assertEquals(1_000, coupon.amount)
        assertTrue(coupon.gifticonId > 0)
        assertTrue(coupon.expiresAtIso.isNotBlank())
    }

    @Test
    fun claimBlankTagCodeReturnsValidation() = runTest {
        val result = repository.claim("   ")

        assertEquals(AppResult.Failure(DomainError.Validation("tagCode")), result)
    }

    @Test
    fun claimCooldownMarkerSimulatesCooldown() = runTest {
        val result = repository.claim("NFC-COOLDOWN-1")

        assertEquals(AppResult.Failure(DomainError.Payment("nfc-cooldown")), result)
    }

    @Test
    fun claimNotFoundMarkerSimulatesNotFound() = runTest {
        val result = repository.claim("NFC-NOTFOUND-1")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun claimInactiveMarkerSimulatesInactive() = runTest {
        val result = repository.claim("NFC-INACTIVE-1")

        assertEquals(AppResult.Failure(DomainError.Payment("nfc-inactive")), result)
    }

    @Test
    fun claimIssuesIncreasingGifticonIds() = runTest {
        val first = repository.claim("NFC-AAAA-1")
        val second = repository.claim("NFC-BBBB-2")

        assertTrue(first is AppResult.Success)
        assertTrue(second is AppResult.Success)
        val firstId = (first as AppResult.Success).data.gifticonId
        val secondId = (second as AppResult.Success).data.gifticonId
        assertTrue(secondId > firstId)
    }
}
