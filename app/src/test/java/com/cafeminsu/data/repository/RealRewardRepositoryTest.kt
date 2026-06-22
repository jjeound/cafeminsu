package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.StampApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
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

class RealRewardRepositoryTest {
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
    fun observeStampCardFetchesSelectedStoreStampDetailWithoutUserIdQuery() = runTest(testDispatcher) {
        server.enqueue(stampDetailResponse(count = 8))
        val repository = realRewardRepository(selectedStore = sampleStore(id = "11"))

        repository.observeStampCard().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val stampCard = (result as AppResult.Success).data
            assertEquals("11", stampCard.userId)
            assertEquals(8, stampCard.currentCount)
            assertEquals(10, stampCard.goalCount)
            assertEquals(1, stampCard.history.size)
            assertEquals("server-stamp-11-1", stampCard.history.single().orderId)
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/stamps/11", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
    }

    @Test
    fun observeStampCardUsesFirstSummaryWhenNoStoreSelected() = runTest(testDispatcher) {
        server.enqueue(stampSummaryListResponse())
        val repository = realRewardRepository(selectedStore = null)

        repository.observeStampCard().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val stampCard = (result as AppResult.Success).data
            assertEquals("12", stampCard.userId)
            assertEquals(6, stampCard.currentCount)
            assertEquals(emptyList<Any>(), stampCard.history)
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/stamps", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
    }

    @Test
    fun observeStampCardReturnsEmptyCardWhenSelectedStoreHasNoStamps() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realRewardRepository(selectedStore = sampleStore(id = "6"))

        repository.observeStampCard().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val stampCard = (result as AppResult.Success).data
            assertEquals("6", stampCard.userId)
            assertEquals(0, stampCard.currentCount)
            assertEquals(10, stampCard.goalCount)
            assertEquals(emptyList<Any>(), stampCard.history)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("/api/stamps/6", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun grantStampsForPaidOrderRequeriesStampCardWithoutGrantEndpoint() = runTest(testDispatcher) {
        server.enqueue(stampDetailResponse(count = 9))
        val repository = realRewardRepository(selectedStore = sampleStore(id = "11"))

        val result = repository.grantStampsForPaidOrder("77")

        assertTrue(result is AppResult.Success)
        assertEquals(9, (result as AppResult.Success).data.currentCount)
        assertEquals(1, server.requestCount)
        assertEquals("/api/stamps/11", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun observeGifticonsFetchesMyGifticonsAndMapsAvailableItems() = runTest(testDispatcher) {
        server.enqueue(myGifticonsResponse())
        val repository = realRewardRepository()

        repository.observeGifticons().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val gifticons = (result as AppResult.Success).data
            assertEquals(listOf("31", "32"), gifticons.map { it.id })
            assertEquals(listOf("₩10,000", "₩5,000"), gifticons.map { it.title })
            assertTrue(gifticons.all { it.status == GifticonStatus.Available })
            assertTrue(gifticons.all { it.qrValue.isEmpty() })
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/gifticons/my", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
    }

    @Test
    fun getGifticonFetchesDetailAndMapsQrStatusAndExpiry() = runTest(testDispatcher) {
        server.enqueue(gifticonDetailResponse(status = "UNUSED", balance = 10_000))
        val repository = realRewardRepository()

        val result = repository.getGifticon("31")

        assertTrue(result is AppResult.Success)
        val gifticon = (result as AppResult.Success).data
        assertEquals("31", gifticon.id)
        assertEquals("₩10,000", gifticon.title)
        assertEquals("sensitive-qr-value", gifticon.qrValue)
        assertEquals("sensitive-qr-value", gifticon.barcodeValue)
        assertEquals(GifticonStatus.Available, gifticon.status)

        val request = server.takeRequest()
        assertEquals("/api/gifticons/31", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
    }

    @Test
    fun markGifticonUsedPostsUseRequestAndReturnsUsedGifticon() = runTest(testDispatcher) {
        server.enqueue(gifticonDetailResponse(status = "UNUSED", balance = 10_000))
        server.enqueue(gifticonUseResponse(status = "USED", balanceAfter = 0))
        val repository = realRewardRepository()

        val result = repository.markGifticonUsed("31")

        assertTrue(result is AppResult.Success)
        val gifticon = (result as AppResult.Success).data
        assertEquals("31", gifticon.id)
        assertEquals(GifticonStatus.Used, gifticon.status)
        assertEquals("sensitive-qr-value", gifticon.qrValue)

        assertEquals("/api/gifticons/31", server.takeRequest().requestUrl?.encodedPath)
        val useRequest = server.takeRequest()
        assertEquals("/api/gifticons/31/use", useRequest.requestUrl?.encodedPath)
        val body = useRequest.body.readUtf8()
        assertTrue(body.contains("\"orderId\":31"))
        assertTrue(body.contains("\"usedAmount\":10000"))
        assertFalse(body.contains("sensitive-qr-value"))
    }

    @Test
    fun notFoundHttpStatusMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realRewardRepository()

        val result = repository.getGifticon("404")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun httpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(401))
        val repository = realRewardRepository()

        repository.observeGifticons().test {
            assertEquals(AppResult.Failure(DomainError.Unauthorized), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unauthenticatedSessionBlocksNetworkBeforeCall() = runTest(testDispatcher) {
        val repository = realRewardRepository(authState = AuthState.Guest)

        repository.observeStampCard().test {
            assertEquals(AppResult.Failure(DomainError.Unauthorized), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, server.requestCount)
    }

    private fun realRewardRepository(
        authState: AuthState = authenticatedState(),
        selectedStore: Store? = sampleStore(id = "11"),
    ): RealRewardRepository {
        val holder = selectedStoreHolderForTest()
        if (selectedStore != null) {
            holder.select(selectedStore)
        }
        return RealRewardRepository(
            stampApi = stampApi(),
            gifticonApi = gifticonApi(),
            selectedStoreHolder = holder,
            sessionStateHolder = SessionStateHolder(authState),
            ioDispatcher = testDispatcher,
        )
    }

    private fun stampApi(): StampApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(StampApi::class.java)

    private fun gifticonApi(): GifticonApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(GifticonApi::class.java)

    private fun stampSummaryListResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "storeId": 12,
                    "storeName": "카페민수 역삼점",
                    "count": 6
                  },
                  {
                    "storeId": 11,
                    "storeName": "카페민수 강남점",
                    "count": 7
                  }
                ]
                """.trimIndent(),
            )

    private fun stampDetailResponse(count: Int): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "storeId": 11,
                  "storeName": "카페민수 강남점",
                  "count": $count,
                  "histories": [
                    {
                      "earnedCount": 2,
                      "createdAt": "2026-06-20T01:15:30Z"
                    }
                  ]
                }
                """.trimIndent(),
            )

    private fun myGifticonsResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "gifticonId": 31,
                    "balance": 10000,
                    "expiresAt": "2026-08-31T15:00:00Z"
                  },
                  {
                    "gifticonId": 32,
                    "balance": 5000,
                    "expiresAt": "2026-07-31T15:00:00Z"
                  }
                ]
                """.trimIndent(),
            )

    private fun gifticonDetailResponse(
        status: String,
        balance: Int,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "gifticonId": 31,
                  "amount": 10000,
                  "balance": $balance,
                  "qrCode": "sensitive-qr-value",
                  "status": "$status",
                  "expiresAt": "2026-08-31T15:00:00Z",
                  "message": "고마워"
                }
                """.trimIndent(),
            )

    private fun gifticonUseResponse(
        status: String,
        balanceAfter: Int,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "balanceAfter": $balanceAfter,
                  "status": "$status"
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

    private fun sampleStore(id: String): Store =
        Store(
            id = id,
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = emptyList(),
        )
}
