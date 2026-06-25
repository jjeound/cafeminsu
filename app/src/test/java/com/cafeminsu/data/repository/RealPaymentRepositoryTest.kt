package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.payment.PgClient
import com.cafeminsu.data.remote.PaymentApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentStatus
import com.cafeminsu.domain.model.UserProfile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealPaymentRepositoryTest {
    private lateinit var server: MockWebServer
    private val testDispatcher = StandardTestDispatcher()

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
    fun payPreparesAuthorizesAndVerifiesPaidAsApproved() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-1", cardAmount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "PAID"))
        val pgClient = FakePgClient(impUid = "imp-success")
        val repository = realPaymentRepository(pgClient = pgClient)

        val result = repository.pay(paymentRequest())

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("31", payment.paymentId)
        assertEquals(PaymentStatus.Approved, payment.status)
        assertEquals(null, payment.approvedAtMillis)
        assertEquals(listOf(PgAuthorizeRequest("merchant-1", 10_000)), pgClient.authorizeRequests)

        val prepare = server.takeRequest()
        assertEquals("/api/payments/prepare", prepare.requestUrl?.encodedPath)
        val prepareBody = prepare.body.readUtf8()
        assertTrue(prepareBody.contains("\"orderId\":77"))
        // 기프티콘 미선택 시 useGifticonId 는 직렬화에서 생략된다.
        assertFalse(prepareBody.contains("useGifticonId"))
        // 카드/기프티콘 분할은 서버가 계산하므로 클라가 cardAmount 를 보내지 않는다.
        assertFalse(prepareBody.contains("cardAmount"))
        assertFalse(prepareBody.contains("paymentMethodToken"))

        val verify = server.takeRequest()
        assertEquals("/api/payments/verify", verify.requestUrl?.encodedPath)
        val verifyBody = verify.body.readUtf8()
        assertTrue(verifyBody.contains("\"impUid\":\"imp-success\""))
        assertTrue(verifyBody.contains("\"merchantUid\":\"merchant-1\""))
    }

    @Test
    fun fullGifticonPrepareReturnsPaidWithoutAuthorizeOrVerify() = runTest(testDispatcher) {
        server.enqueue(
            prepareResponse(
                merchantUid = "merchant-1",
                cardAmount = 0,
                status = "PAID",
                gifticonAmount = 10_000,
                paymentId = 9,
            ),
        )
        val pgClient = FakePgClient()
        val repository = realPaymentRepository(pgClient = pgClient)

        val result = repository.pay(paymentRequest(amount = 0, useGifticonId = 45))

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("9", payment.paymentId)
        assertEquals(PaymentStatus.Approved, payment.status)
        // 전액 기프티콘은 카카오페이 authorize / verify 를 호출하지 않는다.
        assertTrue(pgClient.authorizeRequests.isEmpty())
        assertEquals(1, server.requestCount)

        val prepare = server.takeRequest()
        assertEquals("/api/payments/prepare", prepare.requestUrl?.encodedPath)
        val prepareBody = prepare.body.readUtf8()
        assertTrue(prepareBody.contains("\"orderId\":77"))
        assertTrue(prepareBody.contains("\"useGifticonId\":45"))
    }

    @Test
    fun splitPaymentAuthorizesServerComputedCardAmount() = runTest(testDispatcher) {
        server.enqueue(
            prepareResponse(
                merchantUid = "merchant-1",
                cardAmount = 7_000,
                status = "READY",
                gifticonAmount = 3_000,
            ),
        )
        server.enqueue(verifyResponse(paymentId = 31, status = "PAID"))
        val pgClient = FakePgClient(impUid = "imp-success")
        val repository = realPaymentRepository(pgClient = pgClient)

        val result = repository.pay(paymentRequest(amount = 7_000, useGifticonId = 45))

        assertTrue(result is AppResult.Success)
        assertEquals(PaymentStatus.Approved, (result as AppResult.Success).data.status)
        // 카드 결제액은 서버가 계산한 cardAmount(7,000)를 사용한다(클라 추정치가 아님).
        assertEquals(listOf(PgAuthorizeRequest("merchant-1", 7_000)), pgClient.authorizeRequests)
    }

    @Test
    fun payMapsFailedVerifyToFailedPayment() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-1", cardAmount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "FAILED"))
        val repository = realPaymentRepository()

        val result = repository.pay(paymentRequest())

        assertTrue(result is AppResult.Success)
        assertEquals(PaymentStatus.Failed, (result as AppResult.Success).data.status)
    }

    @Test
    fun repeatedPayWithSameIdempotencyKeyReusesMerchantUid() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-original", cardAmount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "FAILED"))
        server.enqueue(verifyResponse(paymentId = 31, status = "FAILED"))
        val pgClient = FakePgClient(impUid = "imp-retry")
        val repository = realPaymentRepository(pgClient = pgClient)
        val request = paymentRequest(idempotencyKey = "same-key")

        repository.pay(request)
        repository.pay(request)

        assertEquals(
            listOf(
                PgAuthorizeRequest("merchant-original", 10_000),
                PgAuthorizeRequest("merchant-original", 10_000),
            ),
            pgClient.authorizeRequests,
        )
        assertEquals("/api/payments/prepare", server.takeRequest().requestUrl?.encodedPath)
        val firstVerify = server.takeRequest().body.readUtf8()
        val secondVerify = server.takeRequest().body.readUtf8()
        assertTrue(firstVerify.contains("\"merchantUid\":\"merchant-original\""))
        assertTrue(secondVerify.contains("\"merchantUid\":\"merchant-original\""))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun readyVerifyReturnsPendingAndDoesNotApproveOptimistically() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-1", cardAmount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "READY"))
        val repository = realPaymentRepository()

        val result = repository.pay(paymentRequest())

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals(PaymentStatus.Pending, payment.status)
        assertEquals(null, payment.approvedAtMillis)
    }

    @Test
    fun getPaymentStatusUsesPaymentDetailForConfirmation() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-1", cardAmount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "READY"))
        server.enqueue(paymentDetailResponse(paymentId = 31, status = "PAID"))
        val repository = realPaymentRepository()
        val request = paymentRequest(idempotencyKey = "status-key")
        repository.pay(request)

        val result = repository.getPaymentStatus(
            orderId = request.orderId,
            idempotencyKey = request.idempotencyKey,
        )

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals(PaymentStatus.Approved, payment.status)
        assertEquals(1_781_918_130_000, payment.approvedAtMillis)

        server.takeRequest()
        server.takeRequest()
        val detail = server.takeRequest()
        assertEquals("/api/payments/31", detail.requestUrl?.encodedPath)
    }

    @Test
    fun unauthenticatedSessionBlocksPaymentBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realPaymentRepository(authState = AuthState.Guest)

        val result = repository.pay(paymentRequest())

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    private fun realPaymentRepository(
        authState: AuthState = authenticatedState(),
        pgClient: PgClient = FakePgClient(),
    ): RealPaymentRepository =
        RealPaymentRepository(
            paymentApi = paymentApi(),
            pgClient = pgClient,
            sessionStateHolder = SessionStateHolder(authState),
            ioDispatcher = testDispatcher,
        )

    private fun paymentApi(): PaymentApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(PaymentApi::class.java)

    private fun paymentRequest(
        idempotencyKey: String = "idem-1",
        amount: Int = 10_000,
        useGifticonId: Long? = null,
    ): PaymentRequest =
        PaymentRequest(
            orderId = "77",
            amount = amount,
            paymentMethodToken = "tok_kakaopay_mock",
            idempotencyKey = idempotencyKey,
            useGifticonId = useGifticonId,
        )

    private fun prepareResponse(
        merchantUid: String,
        cardAmount: Int,
        status: String = "READY",
        gifticonAmount: Int = 0,
        paymentId: Long? = null,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                buildString {
                    append("{")
                    append("\"merchantUid\": \"$merchantUid\",")
                    append("\"amount\": $cardAmount,")
                    append("\"cardAmount\": $cardAmount,")
                    append("\"gifticonAmount\": $gifticonAmount,")
                    if (paymentId != null) {
                        append("\"paymentId\": $paymentId,")
                    }
                    append("\"status\": \"$status\"")
                    append("}")
                },
            )

    private fun verifyResponse(
        paymentId: Long,
        status: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "paymentId": $paymentId,
                  "status": "$status"
                }
                """.trimIndent(),
            )

    private fun paymentDetailResponse(
        paymentId: Long,
        status: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "paymentId": $paymentId,
                  "orderId": 77,
                  "method": "CARD",
                  "amount": 10000,
                  "status": "$status",
                  "paidAt": "2026-06-20T01:15:30Z"
                }
                """.trimIndent(),
            )

    private fun authenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "42",
                displayName = "민수",
                phoneLast4 = null,
            ),
        )
}

private data class PgAuthorizeRequest(
    val merchantUid: String,
    val amount: Int,
)

private class FakePgClient(
    private val impUid: String = "imp-success",
    private val result: AppResult<String> = AppResult.Success(impUid),
) : PgClient {
    val authorizeRequests = mutableListOf<PgAuthorizeRequest>()

    override suspend fun authorize(
        merchantUid: String,
        amount: Int,
    ): AppResult<String> {
        authorizeRequests += PgAuthorizeRequest(merchantUid, amount)
        return result
    }
}
