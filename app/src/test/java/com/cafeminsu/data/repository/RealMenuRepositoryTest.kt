package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealMenuRepositoryTest {
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
    fun observeCategoriesDerivesDistinctCategoriesSortedByFirstAppearance() = runTest(testDispatcher) {
        server.enqueue(menuListResponse())
        val repository = realMenuRepository()

        repository.observeCategories().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val categories = (result as AppResult.Success).data
            assertEquals(listOf("디저트", "커피", "티"), categories.map { it.id })
            assertEquals(listOf("디저트", "커피", "티"), categories.map { it.name })
            assertEquals(listOf(1, 2, 3), categories.map { it.sortOrder })
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/stores/11/menus", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("category"))
    }

    @Test
    fun observeMenusFetchesSelectedStoreMenusAndMapsSoldOutFlag() = runTest(testDispatcher) {
        server.enqueue(menuListResponse())
        val repository = realMenuRepository()

        repository.observeMenus("커피").test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val menus = (result as AppResult.Success).data
            assertEquals(listOf("101", "102", "103", "104"), menus.map { it.id })
            assertEquals("커피", menus[1].categoryId)
            assertEquals(5_500, menus[1].basePrice)
            assertEquals(false, menus[1].isSoldOut)
            assertEquals(true, menus[2].isSoldOut)
            assertTrue(menus.all { it.options.isEmpty() })
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("커피", request.requestUrl?.queryParameter("category"))
    }

    @Test
    fun observeMenusEmitsEmptySuccessForEmptyServerList() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": []
                    }
                    """.trimIndent(),
                ),
        )
        val repository = realMenuRepository()

        repository.observeMenus().test {
            val result = awaitItem()

            assertEquals(AppResult.Success(emptyList<com.cafeminsu.domain.model.MenuItem>()), result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeMenusReturnsEmptySuccessWhenNoStoreSelected() = runTest(testDispatcher) {
        // 로그인 직후 선택 매장이 없을 때 홈이 메뉴 Failure로 에러 화면에 빠지지 않도록 빈 목록 폴백.
        val repository = RealMenuRepository(
            menuApi = menuApi(),
            selectedStoreHolder = SelectedStoreHolder(),
            ioDispatcher = testDispatcher,
        )

        repository.observeMenus().test {
            assertEquals(
                AppResult.Success(emptyList<com.cafeminsu.domain.model.MenuItem>()),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMenuMapsDetailOptionsIntoOptionGroups() = runTest(testDispatcher) {
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
                        "id": 101,
                        "name": "바닐라라떼",
                        "description": "부드러운 라떼",
                        "price": 5500,
                        "category": "커피",
                        "imageUrl": "https://cdn.example/latte.png",
                        "isAvailable": true,
                        "options": [
                          {
                            "id": 1,
                            "group": "온도",
                            "name": "HOT",
                            "additionalPrice": 0,
                            "isDefault": true
                          },
                          {
                            "id": 2,
                            "group": "온도",
                            "name": "ICE",
                            "additionalPrice": 0,
                            "isDefault": false
                          },
                          {
                            "id": 3,
                            "group": "샷 추가",
                            "name": "+1샷",
                            "additionalPrice": 500,
                            "isDefault": false
                          }
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val repository = realMenuRepository()

        val result = repository.getMenu("101")

        assertTrue(result is AppResult.Success)
        val menu = (result as AppResult.Success).data
        assertEquals("101", menu.id)
        assertEquals("커피", menu.categoryId)
        assertEquals("바닐라라떼", menu.name)
        assertEquals("부드러운 라떼", menu.description)
        assertEquals(2, menu.options.size)
        assertEquals("온도", menu.options[0].name)
        assertEquals(listOf("HOT", "ICE"), menu.options[0].options.map { it.name })
        assertEquals("샷 추가", menu.options[1].name)
        assertEquals(500, menu.options[1].options.single().extraPrice)
    }

    @Test
    fun getMenuReturnsNotFoundForMissingMenu() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realMenuRepository()

        val result = repository.getMenu("999")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    private fun realMenuRepository(): RealMenuRepository {
        val holder = SelectedStoreHolder()
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
        return RealMenuRepository(
            menuApi = menuApi(),
            selectedStoreHolder = holder,
            ioDispatcher = testDispatcher,
        )
    }

    private fun menuApi(): MenuApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(MenuApi::class.java)

    private fun menuListResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "isSuccess": true,
                  "code": 200,
                  "message": "OK",
                  "result": [
                    {
                      "id": 101,
                      "name": "바스크 치즈케이크",
                      "price": 6800,
                      "category": "디저트",
                      "imageUrl": "https://cdn.example/cake.png",
                      "isAvailable": true
                    },
                    {
                      "id": 102,
                      "name": "바닐라라떼",
                      "price": 5500,
                      "category": "커피",
                      "imageUrl": "https://cdn.example/latte.png",
                      "isAvailable": true
                    },
                    {
                      "id": 103,
                      "name": "아인슈페너",
                      "price": 6000,
                      "category": "커피",
                      "imageUrl": "https://cdn.example/einspanner.png",
                      "isAvailable": false
                    },
                    {
                      "id": 104,
                      "name": "유자차",
                      "price": 4800,
                      "category": "티",
                      "imageUrl": "https://cdn.example/tea.png",
                      "isAvailable": true
                    }
                  ]
                }
                """.trimIndent(),
            )
}
