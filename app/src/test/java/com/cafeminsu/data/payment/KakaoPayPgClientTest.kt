package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.KakaoPayApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KakaoPayPgClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authorizeRunsReadyRedirectApproveAndReturnsPaymentToken() = runTest {
        server.enqueue(readyResponse(tid = "T123", redirectUrl = "https://kakao/redirect"))
        server.enqueue(approveResponse(paymentToken = "imp_kakao_1"))
        val bridge = FakeRedirectBridge(pgToken = "pg_token_1")
        val client = kakaoPayPgClient(bridge)

        val result = client.authorize(merchantUid = "merchant-1", amount = 10_000)

        assertTrue(result is AppResult.Success)
        assertEquals("imp_kakao_1", (result as AppResult.Success).data)
        assertEquals(listOf("https://kakao/redirect"), bridge.requestedUrls)

        val ready = server.takeRequest()
        assertEquals("/api/payments/kakaopay/ready", ready.requestUrl?.encodedPath)
        val readyBody = ready.body.readUtf8()
        assertTrue(readyBody.contains("\"merchantUid\":\"merchant-1\""))
        assertTrue(readyBody.contains("\"amount\":10000"))

        val approve = server.takeRequest()
        assertEquals("/api/payments/kakaopay/approve", approve.requestUrl?.encodedPath)
        val approveBody = approve.body.readUtf8()
        assertTrue(approveBody.contains("\"tid\":\"T123\""))
        assertTrue(approveBody.contains("\"pgToken\":\"pg_token_1\""))
        assertTrue(approveBody.contains("\"merchantUid\":\"merchant-1\""))
    }

    @Test
    fun approveHttpFailureMapsToFailure() = runTest {
        server.enqueue(readyResponse(tid = "T123", redirectUrl = "https://kakao/redirect"))
        server.enqueue(MockResponse().setResponseCode(500))
        val client = kakaoPayPgClient(FakeRedirectBridge(pgToken = "pg_token_1"))

        val result = client.authorize(merchantUid = "merchant-1", amount = 10_000)

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }

    @Test
    fun redirectBridgeFailurePropagatesAndSkipsApprove() = runTest {
        server.enqueue(readyResponse(tid = "T123", redirectUrl = "https://kakao/redirect"))
        val client = kakaoPayPgClient(
            FakeRedirectBridge(result = AppResult.Failure(DomainError.Payment("redirect-cancelled"))),
        )

        val result = client.authorize(merchantUid = "merchant-1", amount = 10_000)

        assertEquals(AppResult.Failure(DomainError.Payment("redirect-cancelled")), result)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun blankMerchantUidFailsValidationBeforeNetwork() = runTest {
        val client = kakaoPayPgClient(FakeRedirectBridge(pgToken = "pg"))

        val result = client.authorize(merchantUid = "", amount = 10_000)

        assertEquals(AppResult.Failure(DomainError.Validation("merchantUid")), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun nonPositiveAmountFailsValidationBeforeNetwork() = runTest {
        val client = kakaoPayPgClient(FakeRedirectBridge(pgToken = "pg"))

        val result = client.authorize(merchantUid = "merchant-1", amount = 0)

        assertEquals(AppResult.Failure(DomainError.Validation("amount")), result)
        assertEquals(0, server.requestCount)
    }

    private fun kakaoPayPgClient(bridge: KakaoPayRedirectBridge): KakaoPayPgClient =
        KakaoPayPgClient(
            kakaoPayApi = kakaoPayApi(),
            redirectBridge = bridge,
        )

    private fun kakaoPayApi(): KakaoPayApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(KakaoPayApi::class.java)

    private fun readyResponse(
        tid: String,
        redirectUrl: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "tid": "$tid",
                  "redirectUrl": "$redirectUrl"
                }
                """.trimIndent(),
            )

    private fun approveResponse(
        paymentToken: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "paymentToken": "$paymentToken"
                }
                """.trimIndent(),
            )
}

private class FakeRedirectBridge(
    pgToken: String = "pg_token",
    private val result: AppResult<String> = AppResult.Success(pgToken),
) : KakaoPayRedirectBridge {
    val requestedUrls = mutableListOf<String>()

    override suspend fun awaitPgToken(redirectUrl: String): AppResult<String> {
        requestedUrls += redirectUrl
        return result
    }
}
