package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.RecommendationApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealRecommendationRepositoryTest {
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
    fun resolvesFirstRecommendedMenuIdToMenuItemDetail() = runTest(testDispatcher) {
        server.enqueue(recommendationResponse())
        server.enqueue(menuDetailResponse())
        val repository = realRecommendationRepository()

        repository.observeTodayRecommendation().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val menu = (result as AppResult.Success).data
            assertEquals("101", menu?.id)
            assertEquals("바닐라라떼", menu?.name)
            assertEquals(5_500, menu?.basePrice)
            assertEquals(false, menu?.isSoldOut)
            awaitComplete()
        }

        assertEquals(
            "/api/stores/11/recommendations/today",
            server.takeRequest().requestUrl?.encodedPath,
        )
        assertEquals("/api/menus/101", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun returnsNullSuccessWhenNoStoreSelected() = runTest(testDispatcher) {
        // 선택 매장이 없으면 서버를 호출하지 않고 null 로 폴백한다(홈은 메뉴 파생으로 대체).
        val repository = RealRecommendationRepository(
            recommendationApi = recommendationApi(),
            menuApi = menuApi(),
            selectedStoreHolder = selectedStoreHolderForTest(),
            ioDispatcher = testDispatcher,
        )

        repository.observeTodayRecommendation().test {
            assertEquals(AppResult.Success(null), awaitItem())
            awaitComplete()
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun returnsNullSuccessForEmptyRecommendations() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{ "recommendations": [] }""".trimIndent()),
        )
        val repository = realRecommendationRepository()

        repository.observeTodayRecommendation().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertNull((result as AppResult.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun propagatesFailureWhenRecommendationCallFails() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realRecommendationRepository()

        repository.observeTodayRecommendation().test {
            assertTrue(awaitItem() is AppResult.Failure)
            awaitComplete()
        }
    }

    @Test
    fun propagatesFailureWhenMenuDetailLookupFails() = runTest(testDispatcher) {
        server.enqueue(recommendationResponse())
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realRecommendationRepository()

        repository.observeTodayRecommendation().test {
            assertTrue(awaitItem() is AppResult.Failure)
            awaitComplete()
        }
    }

    private fun realRecommendationRepository(): RealRecommendationRepository {
        val holder = selectedStoreHolderForTest()
        holder.select(
            Store(
                id = "11",
                name = "카페민수 강남점",
                address = "서울 강남구 테헤란로 134",
                phone = "02-1234-5678",
                distanceMeters = 120,
                latitude = 37.498,
                longitude = 127.028,
                status = StoreStatus.Open,
                closingTimeLabel = "22:00 마감",
                amenities = emptyList(),
            ),
        )
        return RealRecommendationRepository(
            recommendationApi = recommendationApi(),
            menuApi = menuApi(),
            selectedStoreHolder = holder,
            ioDispatcher = testDispatcher,
        )
    }

    private fun recommendationApi(): RecommendationApi =
        retrofit().create(RecommendationApi::class.java)

    private fun menuApi(): MenuApi =
        retrofit().create(MenuApi::class.java)

    private fun retrofit() =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        )

    private fun recommendationResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "recommendations": [
                    { "menuId": 101, "quantity": 1, "optionIds": [] },
                    { "menuId": 102, "quantity": 1, "optionIds": [] }
                  ]
                }
                """.trimIndent(),
            )

    private fun menuDetailResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "id": 101,
                  "name": "바닐라라떼",
                  "description": "부드러운 라떼",
                  "price": 5500,
                  "category": "커피",
                  "imageUrl": "https://cdn.example/latte.png",
                  "isAvailable": true,
                  "options": []
                }
                """.trimIndent(),
            )
}
