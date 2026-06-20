package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgClientTest {
    @Test
    fun mockPgClientReturnsImpUidForValidMerchantUid() = runBlocking {
        val result = MockPgClient().authorize(
            merchantUid = "merchant-123",
            amount = 10_000,
        )

        assertTrue(result is AppResult.Success)
        assertEquals("imp_mock_merchant-123", (result as AppResult.Success).data)
    }

    @Test
    fun mockPgClientReturnsFailureImpUidForFailureMerchantUid() = runBlocking {
        val result = MockPgClient().authorize(
            merchantUid = "merchant-fail-123",
            amount = 10_000,
        )

        assertTrue(result is AppResult.Success)
        assertEquals("imp_mock_fail_merchant-fail-123", (result as AppResult.Success).data)
    }

    @Test
    fun mockPgClientRejectsInvalidAmount() = runBlocking {
        val result = MockPgClient().authorize(
            merchantUid = "merchant-123",
            amount = 0,
        )

        assertEquals(AppResult.Failure(DomainError.Validation("amount")), result)
    }
}
