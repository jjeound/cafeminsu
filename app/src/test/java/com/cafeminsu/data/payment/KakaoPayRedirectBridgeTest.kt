package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoPayRedirectBridgeTest {
    @Test
    fun noOpBridgeFailsWithoutRedirectImplementation() = runBlocking {
        val result = NoOpKakaoPayRedirectBridge().awaitPgToken(
            redirectUrl = "https://online-pay.kakao.com/redirect",
        )

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is DomainError.Payment)
    }
}
