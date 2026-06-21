package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.remote.StoreApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.StoreStatus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealStoreRepositoryTest {
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
    fun observeNearbyStoresEmitsPartialListThenEnrichesWithCoordinates() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "stores": [
                          {
                            "id": 7,
                            "name": "카페민수 강남점",
                            "address": "서울 강남구 테헤란로 134",
                            "imageUrl": "https://cdn.example/store.png"
                          }
                        ],
                        "total": 1
                      }
                    }
                    """.trimIndent(),
                ),
        )
        server.enqueue(storeDetailResponse())
        val repository = realStoreRepository()

        repository.observeNearbyStores("강남").test {
            // 1차: 목록 API 결과(좌표 미제공이라 0,0).
            val partial = awaitItem()
            assertTrue(partial is AppResult.Success)
            val partialStore = (partial as AppResult.Success).data.single()
            assertEquals("7", partialStore.id)
            assertEquals("카페민수 강남점", partialStore.name)
            assertEquals("서울 강남구 테헤란로 134", partialStore.address)
            assertEquals("", partialStore.phone)
            assertEquals(0, partialStore.distanceMeters)
            assertEquals(0.0, partialStore.latitude, 0.0)
            assertEquals(0.0, partialStore.longitude, 0.0)
            assertEquals(StoreStatus.Open, partialStore.status)
            assertTrue(partialStore.amenities.isEmpty())

            // 2차: 상세 API로 좌표를 채운 목록(지도 마커용). 목록 메타데이터는 유지.
            val located = awaitItem()
            assertTrue(located is AppResult.Success)
            val locatedStore = (located as AppResult.Success).data.single()
            assertEquals("7", locatedStore.id)
            assertEquals("카페민수 강남점", locatedStore.name)
            assertEquals("서울 강남구 테헤란로 134", locatedStore.address)
            assertEquals(37.498, locatedStore.latitude, 0.0)
            assertEquals(127.028, locatedStore.longitude, 0.0)

            cancelAndIgnoreRemainingEvents()
        }

        val listRequest = server.takeRequest()
        assertEquals("/api/stores", listRequest.requestUrl?.encodedPath)
        assertEquals("강남", listRequest.requestUrl?.queryParameter("keyword"))
        assertEquals("0", listRequest.requestUrl?.queryParameter("page"))
        assertEquals("20", listRequest.requestUrl?.queryParameter("size"))

        val detailRequest = server.takeRequest()
        assertEquals("/api/stores/7", detailRequest.requestUrl?.encodedPath)
    }

    @Test
    fun observeNearbyStoresKeepsPartialStoreWhenCoordinateLookupFails() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "stores": [
                          {
                            "id": 7,
                            "name": "카페민수 강남점",
                            "address": "서울 강남구 테헤란로 134",
                            "imageUrl": null
                          }
                        ],
                        "total": 1
                      }
                    }
                    """.trimIndent(),
                ),
        )
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realStoreRepository()

        repository.observeNearbyStores("강남").test {
            val partial = awaitItem()
            assertTrue(partial is AppResult.Success)
            assertEquals(0.0, (partial as AppResult.Success).data.single().latitude, 0.0)

            // 좌표 조회가 실패해도 목록 자체는 유지된다(추가 emit 없음).
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getStoreFetchesDetailAndSelectStoreUpdatesSelectedStore() = runTest(testDispatcher) {
        server.enqueue(storeDetailResponse())
        server.enqueue(storeDetailResponse())
        val repository = realStoreRepository()

        val detailResult = repository.getStore("7")

        assertTrue(detailResult is AppResult.Success)
        val store = (detailResult as AppResult.Success).data
        assertEquals("7", store.id)
        assertEquals("카페민수 강남점", store.name)
        assertEquals("02-1234-5678", store.phone)
        assertEquals(37.498, store.latitude, 0.0)
        assertEquals(127.028, store.longitude, 0.0)
        assertEquals("09:00-22:00", store.closingTimeLabel)

        repository.observeSelectedStore().test {
            assertEquals(null, awaitItem())

            val selectResult = repository.selectStore("7")

            assertTrue(selectResult is AppResult.Success)
            assertEquals("7", awaitItem()?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeNearbyStoresEmitsEmptySuccessForEmptyServerList() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "stores": [],
                        "total": 0
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val repository = realStoreRepository()

        repository.observeNearbyStores().test {
            val result = awaitItem()

            assertEquals(AppResult.Success(emptyList<com.cafeminsu.domain.model.Store>()), result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun realStoreRepository(): RealStoreRepository =
        RealStoreRepository(
            storeApi = storeApi(),
            selectedStoreHolder = SelectedStoreHolder(),
            ioDispatcher = testDispatcher,
        )

    private fun storeApi(): StoreApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(StoreApi::class.java)

    private fun storeDetailResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "isSuccess": true,
                  "code": 200,
                  "message": "OK",
                  "result": {
                    "id": 7,
                    "name": "카페민수 강남점",
                    "address": "서울 강남구 테헤란로 134",
                    "latitude": 37.498,
                    "longitude": 127.028,
                    "phone": "02-1234-5678",
                    "businessHours": "09:00-22:00",
                    "imageUrl": "https://cdn.example/store.png"
                  }
                }
                """.trimIndent(),
            )
}
