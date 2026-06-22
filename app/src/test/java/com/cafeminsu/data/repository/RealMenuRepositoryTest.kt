package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.menu.MenuLocalDataSource
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.MenuItem
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
    fun observeMenusReturnsEmptySuccessAndSkipsCacheWhenNoStoreSelected() = runTest(testDispatcher) {
        // 로그인 직후 선택 매장이 없을 때 홈이 메뉴 Failure로 에러 화면에 빠지지 않도록 빈 목록 폴백.
        // 선택 매장이 없으면 캐시는 읽지도 쓰지도 않는다.
        val cache = FakeMenuLocalDataSource()
        val repository = RealMenuRepository(
            menuApi = menuApi(),
            selectedStoreHolder = selectedStoreHolderForTest(),
            localDataSource = cache,
            ioDispatcher = testDispatcher,
        )

        repository.observeMenus().test {
            assertEquals(
                AppResult.Success(emptyList<MenuItem>()),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, cache.replaceCount)
        assertEquals(0, cache.cachedCount)
    }

    @Test
    fun observeMenusWritesThroughToCacheOnSuccess() = runTest(testDispatcher) {
        server.enqueue(menuListResponse())
        val cache = FakeMenuLocalDataSource()
        val repository = realMenuRepository(local = cache)

        repository.observeMenus("커피").test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, cache.replaceCount)
        assertEquals(
            listOf("101", "102", "103", "104"),
            cache.storedFor("11").map { it.id },
        )
    }

    @Test
    fun observeMenusFallsBackToCacheOnNetworkFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val cache = FakeMenuLocalDataSource(initial = mapOf("11" to cachedMenus()))
        val repository = realMenuRepository(local = cache)

        repository.observeMenus().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(listOf("201", "202"), (result as AppResult.Success).data.map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun observeMenusOfflineFallbackFiltersByCategory() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val cache = FakeMenuLocalDataSource(initial = mapOf("11" to cachedMenus()))
        val repository = realMenuRepository(local = cache)

        repository.observeMenus("커피").test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(listOf("201"), (result as AppResult.Success).data.map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun observeMenusEmitsFailureWhenNetworkFailsAndCacheEmpty() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realMenuRepository(local = FakeMenuLocalDataSource())

        repository.observeMenus().test {
            assertTrue(awaitItem() is AppResult.Failure)
            awaitComplete()
        }
    }

    @Test
    fun observeCategoriesFallsBackToCacheDerivedCategoriesOnFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val cache = FakeMenuLocalDataSource(initial = mapOf("11" to cachedMenus()))
        val repository = realMenuRepository(local = cache)

        repository.observeCategories().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(listOf("커피", "디저트"), (result as AppResult.Success).data.map { it.id })
            awaitComplete()
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

    private fun realMenuRepository(
        local: MenuLocalDataSource = FakeMenuLocalDataSource(),
    ): RealMenuRepository {
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
        return RealMenuRepository(
            menuApi = menuApi(),
            selectedStoreHolder = holder,
            localDataSource = local,
            ioDispatcher = testDispatcher,
        )
    }

    private fun cachedMenus(): List<MenuItem> =
        listOf(
            cachedMenu(id = "201", category = "커피"),
            cachedMenu(id = "202", category = "디저트"),
        )

    private fun cachedMenu(id: String, category: String): MenuItem =
        MenuItem(
            id = id,
            categoryId = category,
            name = "캐시 메뉴 $id",
            description = "",
            basePrice = 5_000,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        )

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

private class FakeMenuLocalDataSource(
    initial: Map<String, List<MenuItem>> = emptyMap(),
) : MenuLocalDataSource {
    private val store = initial.toMutableMap()
    var replaceCount: Int = 0
        private set
    var cachedCount: Int = 0
        private set

    override suspend fun cachedMenus(storeId: String): List<MenuItem> {
        cachedCount++
        return store[storeId].orEmpty()
    }

    override suspend fun replaceMenus(storeId: String, menus: List<MenuItem>) {
        store[storeId] = menus
        replaceCount++
    }

    fun storedFor(storeId: String): List<MenuItem> = store[storeId].orEmpty()
}
