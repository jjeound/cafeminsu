package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GifticonStatus
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

class RealGiftRepositoryTest {
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
    fun sendGiftPurchasesOnceAndMapsGiftIdAndSentAt() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 55))
        val repository = realGiftRepository(nowMillis = { 1_782_012_345_000L })

        val result = repository.sendGift(
            giftRequest(message = "오늘 하루 수고 많았어"),
        )

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("55", gift.giftId)
        assertEquals(1_782_012_345_000L, gift.sentAtMillis)

        // 별도 share API 호출 없이 구매 1회만 수행한다.
        assertEquals(1, server.requestCount)
        val purchase = server.takeRequest()
        assertEquals("/api/gifticons", purchase.requestUrl?.encodedPath)
        assertEquals(null, purchase.requestUrl?.queryParameter("userId"))
        val purchaseBody = purchase.body.readUtf8()
        assertTrue(purchaseBody.contains("\"amount\":10000"))
        assertTrue(purchaseBody.contains("\"message\":\"오늘 하루 수고 많았어\""))
        // 카카오톡 단일 채널: 수신자 미지정 구매. 수신자 식별자는 서버로 보내지 않는다.
        assertFalse(purchaseBody.contains("receiverId"))
        assertFalse(purchaseBody.contains("receiverPhone"))
        assertFalse(purchaseBody.contains("qr-sensitive-value"))
    }

    @Test
    fun sendGiftMapsShareLinkAndClaimCode() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 60))
        val repository = realGiftRepository()

        val result = repository.sendGift(giftRequest())

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("https://cafeminsu.example/gift?code=GFT-XXXX-XXXX", gift.shareLink)
        assertEquals("GFT-XXXX-XXXX", gift.claimCode)
    }

    @Test
    fun purchaseFailureReturnsFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realGiftRepository()

        val result = repository.sendGift(giftRequest())

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
        assertEquals(1, server.requestCount)
        assertEquals("/api/gifticons", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun invalidAmountBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realGiftRepository()

        val result = repository.sendGift(giftRequest(amount = 0))

        assertEquals(AppResult.Failure(DomainError.Validation("amount")), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun guestSessionBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realGiftRepository(authState = AuthState.Guest)

        val result = repository.sendGift(giftRequest())

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun claimGiftMapsResponseToGifticon() = runTest(testDispatcher) {
        server.enqueue(claimResponse())
        val repository = realGiftRepository()

        val result = repository.claimGift("GFT-1234-5678")

        assertTrue(result is AppResult.Success)
        val gifticon = (result as AppResult.Success).data
        assertEquals("123", gifticon.id)
        assertEquals("금액형 기프티콘 10,000원", gifticon.title)
        assertEquals("barcode-sensitive-value", gifticon.barcodeValue)
        assertEquals("qr-sensitive-value", gifticon.qrValue)
        assertEquals(1_750_000_000_000L, gifticon.expiresAtMillis)
        assertEquals(GifticonStatus.Available, gifticon.status)

        val request = server.takeRequest()
        assertEquals("/api/gifticons/claim", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
        assertTrue(request.body.readUtf8().contains("\"claimCode\":\"GFT-1234-5678\""))
    }

    @Test
    fun claimGiftBlankCodeBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realGiftRepository()

        val result = repository.claimGift("   ")

        assertEquals(AppResult.Failure(DomainError.Validation("claimCode")), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun claimGiftGuestSessionBlocksBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realGiftRepository(authState = AuthState.Guest)

        val result = repository.claimGift("GFT-1234-5678")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun claimGiftAlreadyClaimedMapsToPaymentError() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(409))
        val repository = realGiftRepository()

        val result = repository.claimGift("GFT-1234-5678")

        assertEquals(AppResult.Failure(DomainError.Payment("already-claimed")), result)
        assertEquals(1, server.requestCount)
        assertEquals("/api/gifticons/claim", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun claimGiftNotFoundMapsToNotFound() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realGiftRepository()

        val result = repository.claimGift("GFT-1234-5678")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    private fun realGiftRepository(
        authState: AuthState = authenticatedState(),
        nowMillis: () -> Long = { 1_782_000_000_000L },
    ): RealGiftRepository =
        RealGiftRepository(
            gifticonApi = gifticonApi(),
            sessionStateHolder = SessionStateHolder(authState),
            ioDispatcher = testDispatcher,
            nowMillis = nowMillis,
        )

    private fun gifticonApi(): GifticonApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(GifticonApi::class.java)

    private fun giftRequest(
        amount: Int = 10_000,
        message: String? = "고마워",
    ): GiftSendRequest =
        GiftSendRequest(
            amount = amount,
            message = message,
        )

    private fun purchaseResponse(gifticonId: Long): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "gifticonId": $gifticonId,
                  "qrCode": "qr-sensitive-value",
                  "merchantUid": "merchant-sensitive-value",
                  "claimCode": "GFT-XXXX-XXXX",
                  "shareLink": "https://cafeminsu.example/gift?code=GFT-XXXX-XXXX"
                }
                """.trimIndent(),
            )

    private fun claimResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "gifticonId": 123,
                  "title": "금액형 기프티콘 10,000원",
                  "barcodeValue": "barcode-sensitive-value",
                  "qrValue": "qr-sensitive-value",
                  "expiresAtMillis": 1750000000000,
                  "status": "AVAILABLE"
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
