package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoPayApiTest {
    @Test
    fun readyRequestKeepsProxyFields() {
        val request = KakaoPayReadyReq(
            merchantUid = "merchant-123",
            amount = 10_000,
        )

        assertEquals("merchant-123", request.merchantUid)
        assertEquals(10_000, request.amount)
    }

    @Test
    fun approveRequestKeepsProxyFields() {
        val request = KakaoPayApproveReq(
            tid = "T1234567890",
            pgToken = "pg_token_abc",
            merchantUid = "merchant-123",
        )

        assertEquals("T1234567890", request.tid)
        assertEquals("pg_token_abc", request.pgToken)
        assertEquals("merchant-123", request.merchantUid)
    }

    @Test
    fun kakaoPayResponseDtosKeepProxyFields() {
        val ready = KakaoPayReadyRes(
            tid = "T1234567890",
            redirectUrl = "https://online-pay.kakao.com/redirect",
        )
        val approve = KakaoPayApproveRes(
            paymentToken = "imp_kakao_123",
        )

        assertEquals("T1234567890", ready.tid)
        assertEquals("https://online-pay.kakao.com/redirect", ready.redirectUrl)
        assertEquals("imp_kakao_123", approve.paymentToken)
    }
}
