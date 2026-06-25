package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.NfcApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealNfcCouponRepositoryTest {
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
    fun claimSuccessMapsResponseToNfcCoupon() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "gifticonId": 123,
                      "amount": 1000,
                      "expiresAt": "2026-12-25T10:30:00",
                      "message": "방문 감사 쿠폰"
                    }
                    """.trimIndent(),
                ),
        )
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertTrue(result is AppResult.Success)
        val coupon = (result as AppResult.Success).data
        assertEquals(123L, coupon.gifticonId)
        assertEquals(1000, coupon.amount)
        assertEquals("2026-12-25T10:30:00", coupon.expiresAtIso)
        assertEquals("방문 감사 쿠폰", coupon.message)

        val request = server.takeRequest()
        assertEquals("/api/nfc/claim", request.requestUrl?.encodedPath)
        assertTrue(request.body.readUtf8().contains("\"tagCode\":\"NFC-AB7K-9QM2\""))
    }

    @Test
    fun claimCooldownMapsToPaymentError() = runTest(testDispatcher) {
        server.enqueue(errorResponse(409, "NFC_CLAIM_COOLDOWN"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.Payment("nfc-cooldown")), result)
        assertEquals("/api/nfc/claim", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun claimTagNotFoundMapsToNotFound() = runTest(testDispatcher) {
        server.enqueue(errorResponse(404, "NFC_TAG_NOT_FOUND"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun claimInactiveTagMapsToPaymentError() = runTest(testDispatcher) {
        server.enqueue(errorResponse(400, "NFC_TAG_INACTIVE"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.Payment("nfc-inactive")), result)
    }

    @Test
    fun claimValidationFailedMapsToValidation() = runTest(testDispatcher) {
        server.enqueue(errorResponse(400, "VALIDATION_FAILED"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.Validation("tagCode")), result)
    }

    @Test
    fun claimUnauthorizedCodeMapsToUnauthorized() = runTest(testDispatcher) {
        server.enqueue(errorResponse(401, "EXPIRED_TOKEN"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
    }

    @Test
    fun claimUnknownErrorCodeFallsBackToHttpStatus() = runTest(testDispatcher) {
        // body code 가 미지의 값이면 HTTP status 폴백(404 → NotFound).
        server.enqueue(errorResponse(404, "SOMETHING_ELSE"))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun claimMissingErrorBodyFallsBackToHttpStatus() = runTest(testDispatcher) {
        // body code 가 없으면 HTTP status 폴백(404 → NotFound).
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realRepository()

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun claimBlankTagCodeBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realRepository()

        val result = repository.claim("   ")

        assertEquals(AppResult.Failure(DomainError.Validation("tagCode")), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun claimGuestSessionBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realRepository(authState = AuthState.Guest)

        val result = repository.claim("NFC-AB7K-9QM2")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    private fun realRepository(
        authState: AuthState = authenticatedState(),
    ): RealNfcCouponRepository =
        RealNfcCouponRepository(
            nfcApi = nfcApi(),
            sessionStateHolder = SessionStateHolder(authState),
            moshi = moshi,
            ioDispatcher = testDispatcher,
        )

    private val moshi: Moshi = createMoshi()

    private fun nfcApi(): NfcApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = moshi,
            okHttpClient = createOkHttpClient(debug = false),
        ).create(NfcApi::class.java)

    private fun errorResponse(code: Int, errorCode: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setBody("""{ "code": "$errorCode", "message": "사유" }""")

    private fun authenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "42",
                displayName = "민수",
                phoneLast4 = null,
            ),
        )
}
