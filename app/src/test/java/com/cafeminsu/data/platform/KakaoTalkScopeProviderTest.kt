package com.cafeminsu.data.platform

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoTalkScopeProviderTest {
    @Test
    fun grantedConsentMapsToSuccess() {
        val result = friendMessageScopeConsentResult(
            tokenPresent = true,
            cancelled = false,
            failed = false,
        )

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun cancelledOrRefusedConsentMapsToUnauthorized() {
        val result = friendMessageScopeConsentResult(
            tokenPresent = false,
            cancelled = true,
            failed = true,
        )

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
    }

    @Test
    fun otherFailureMapsToUnknown() {
        val result = friendMessageScopeConsentResult(
            tokenPresent = false,
            cancelled = false,
            failed = true,
        )

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }

    @Test
    fun missingTokenWithoutErrorMapsToUnknown() {
        val result = friendMessageScopeConsentResult(
            tokenPresent = false,
            cancelled = false,
            failed = false,
        )

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
