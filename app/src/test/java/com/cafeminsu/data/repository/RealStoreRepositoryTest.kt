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
    fun observeNearbyStoresSearchesServerAndMapsPartialStoreList() = runTest(testDispatcher) {
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
        val repository = realStoreRepository()

        repository.observeNearbyStores("강남").test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val store = (result as AppResult.Success).data.single()
            assertEquals("7", store.id)
            assertEquals("카페민수 강남점", store.name)
            assertEquals("서울 강남구 테헤란로 134", store.address)
            assertEquals("", store.phone)
            assertEquals(0, store.distanceMeters)
            assertEquals(0.0, store.latitude, 0.0)
            assertEquals(0.0, store.longitude, 0.0)
            assertEquals(StoreStatus.Open, store.status)
            assertTrue(store.amenities.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/stores", request.requestUrl?.encodedPath)
        assertEquals("강남", request.requestUrl?.queryParameter("keyword"))
        assertEquals("0", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("size"))
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
