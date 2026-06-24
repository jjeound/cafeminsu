package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
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
    fun sendGiftPurchasesThenSharesAndMapsGiftIdAndSentAt() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 55))
        server.enqueue(shareResponse())
        val repository = realGiftRepository(nowMillis = { 1_782_012_345_000L })

        val result = repository.sendGift(
            giftRequest(
                channel = GiftChannel.KakaoTalk,
                recipientRef = "42",
                message = "오늘 하루 수고 많았어",
            ),
        )

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("55", gift.giftId)
        assertEquals(1_782_012_345_000L, gift.sentAtMillis)

        val purchase = server.takeRequest()
        assertEquals("/api/gifticons", purchase.requestUrl?.encodedPath)
        assertEquals(null, purchase.requestUrl?.queryParameter("userId"))
        val purchaseBody = purchase.body.readUtf8()
        assertTrue(purchaseBody.contains("\"amount\":10000"))
        assertTrue(purchaseBody.contains("\"receiverId\":42"))
        assertTrue(purchaseBody.contains("\"message\":\"오늘 하루 수고 많았어\""))
        assertFalse(purchaseBody.contains("shareLink"))
        assertFalse(purchaseBody.contains("qr-sensitive-value"))

        val share = server.takeRequest()
        assertEquals("/api/gifticons/55/share", share.requestUrl?.encodedPath)
        assertEquals(null, share.requestUrl?.queryParameter("userId"))
        assertEquals("", share.body.readUtf8())
    }

    @Test
    fun sendGiftKakaoChannelMapsShareLinkAndDeepLink() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 60))
        server.enqueue(shareResponse())
        val repository = realGiftRepository()

        val result = repository.sendGift(
            giftRequest(channel = GiftChannel.KakaoTalk, recipientRef = "42"),
        )

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("https://cafeminsu.example/gift/secret", gift.shareLink)
        assertEquals("cafeminsu://gift/secret", gift.deepLink)
    }

    @Test
    fun sendGiftMapsSmsRecipientToReceiverPhone() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 56))
        server.enqueue(shareResponse())
        val repository = realGiftRepository()

        val result = repository.sendGift(
            giftRequest(
                channel = GiftChannel.Sms,
                recipientRef = "010-1234-5678",
                message = null,
            ),
        )

        assertTrue(result is AppResult.Success)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"receiverPhone\":\"010-1234-5678\""))
        assertFalse(body.contains("receiverId"))
    }

    @Test
    fun purchaseFailureReturnsFailureAndDoesNotShare() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realGiftRepository()

        val result = repository.sendGift(giftRequest())

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
        assertEquals(1, server.requestCount)
        assertEquals("/api/gifticons", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun shareFailureReturnsFailureAfterPurchase() = runTest(testDispatcher) {
        server.enqueue(purchaseResponse(gifticonId = 57))
        server.enqueue(MockResponse().setResponseCode(401))
        val repository = realGiftRepository()

        val result = repository.sendGift(giftRequest())

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals("/api/gifticons", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/api/gifticons/57/share", server.takeRequest().requestUrl?.encodedPath)
        assertEquals(2, server.requestCount)
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
        channel: GiftChannel = GiftChannel.KakaoTalk,
        recipientRef: String = "42",
        message: String? = "고마워",
    ): GiftSendRequest =
        GiftSendRequest(
            amount = amount,
            channel = channel,
            recipientRef = recipientRef,
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
                  "merchantUid": "merchant-sensitive-value"
                }
                """.trimIndent(),
            )

    private fun shareResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "shareLink": "https://cafeminsu.example/gift/secret",
                  "deepLink": "cafeminsu://gift/secret"
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
