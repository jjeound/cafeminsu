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
        server.enqueue(prepareResponse(merchantUid = "merchant-1", amount = 10_000))
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
        assertTrue(prepareBody.contains("\"cardAmount\":10000"))
        assertFalse(prepareBody.contains("paymentMethodToken"))

        val verify = server.takeRequest()
        assertEquals("/api/payments/verify", verify.requestUrl?.encodedPath)
        val verifyBody = verify.body.readUtf8()
        assertTrue(verifyBody.contains("\"impUid\":\"imp-success\""))
        assertTrue(verifyBody.contains("\"merchantUid\":\"merchant-1\""))
    }

    @Test
    fun payMapsFailedVerifyToFailedPayment() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-1", amount = 10_000))
        server.enqueue(verifyResponse(paymentId = 31, status = "FAILED"))
        val repository = realPaymentRepository()

        val result = repository.pay(paymentRequest())

        assertTrue(result is AppResult.Success)
        assertEquals(PaymentStatus.Failed, (result as AppResult.Success).data.status)
    }

    @Test
    fun repeatedPayWithSameIdempotencyKeyReusesMerchantUid() = runTest(testDispatcher) {
        server.enqueue(prepareResponse(merchantUid = "merchant-original", amount = 10_000))
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
        server.enqueue(prepareResponse(merchantUid = "merchant-1", amount = 10_000))
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
        server.enqueue(prepareResponse(merchantUid = "merchant-1", amount = 10_000))
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
    ): PaymentRequest =
        PaymentRequest(
            orderId = "77",
            amount = 10_000,
            paymentMethodToken = "tok_credit_card_mock",
            idempotencyKey = idempotencyKey,
        )

    private fun prepareResponse(
        merchantUid: String,
        amount: Int,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "merchantUid": "$merchantUid",
                  "amount": $amount
                }
                """.trimIndent(),
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
