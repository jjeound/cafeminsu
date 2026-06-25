package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.platform.MenuImageData
import com.cafeminsu.data.platform.MenuImageReader
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.OwnerMenuApi
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.NewMenuDraft
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
import retrofit2.Retrofit

class RealOwnerMenuRepositoryTest {
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
    fun observeManagedMenusResolvesStoreThenMapsMenus() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus("coffee").test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            val menus = (result as AppResult.Success).data
            assertEquals(listOf("101", "102"), menus.map { it.id })

            val americano = menus.first()
            assertEquals("아메리카노", americano.name)
            assertEquals(4_500, americano.basePrice)
            assertEquals("coffee", americano.categoryId)
            assertFalse(americano.isSoldOut)
            // isAvailable=false 메뉴는 품절로 매핑된다.
            assertTrue(menus[1].isSoldOut)
            cancelAndIgnoreRemainingEvents()
        }

        val storesRequest = server.takeRequest()
        assertEquals("/api/stores/my", storesRequest.requestUrl?.encodedPath)
        val menusRequest = server.takeRequest()
        assertEquals("/api/stores/7/menus", menusRequest.requestUrl?.encodedPath)
        assertEquals("coffee", menusRequest.requestUrl?.queryParameter("category"))
    }

    @Test
    fun emptyMyStoresReturnsEmptyListWithoutMenusCall() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertTrue((result as AppResult.Success).data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // stores/my 가 비어 있으면 메뉴 호출 없이 안전하게 빈 목록을 낸다.
        assertEquals(1, server.requestCount)
        assertEquals("/api/stores/my", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun menusHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus().test {
            assertTrue(awaitItem() is AppResult.Failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutPatchesAvailabilityAndReflectsOnObservedMenu() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val repository = realOwnerMenuRepository()

        // 마지막 관측분에 반영하기 위해 먼저 목록을 관측한다.
        repository.observeManagedMenus().test {
            assertTrue(awaitItem() is AppResult.Success)
            cancelAndIgnoreRemainingEvents()
        }
        server.takeRequest() // stores/my
        server.takeRequest() // menus

        val result = repository.setSoldOut(menuItemId = "101", soldOut = true)

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        assertTrue(updated.isSoldOut)
        // 관측분의 이름/가격은 보존된다.
        assertEquals("아메리카노", updated.name)
        assertEquals(4_500, updated.basePrice)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/menus/101/availability", request.requestUrl?.encodedPath)
        // 품절(soldOut=true) → 서버 isAvailable=false.
        assertTrue(request.body.readUtf8().contains("\"isAvailable\":false"))
    }

    @Test
    fun setSoldOutSuccessEmitsUpdatedMenuToActiveObserver() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val repository = realOwnerMenuRepository()

        // 관측을 유지한 채 토글하면 변경이 화면(관측자)으로 전파돼야 한다.
        repository.observeManagedMenus().test {
            val initial = awaitItem()
            assertTrue(initial is AppResult.Success)
            assertFalse((initial as AppResult.Success).data.first { it.id == "101" }.isSoldOut)

            val result = repository.setSoldOut(menuItemId = "101", soldOut = true)
            assertTrue(result is AppResult.Success)

            val updated = awaitItem()
            assertTrue(updated is AppResult.Success)
            val americano = (updated as AppResult.Success).data.first { it.id == "101" }
            assertTrue(americano.isSoldOut)
            // 토글이 항목의 이름/가격을 보존하는지 확인한다.
            assertEquals("아메리카노", americano.name)
            assertEquals(4_500, americano.basePrice)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutFailureKeepsSnapshotUnchangedForObserver() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus().test {
            val initial = awaitItem()
            assertTrue(initial is AppResult.Success)
            assertFalse((initial as AppResult.Success).data.first { it.id == "101" }.isSoldOut)

            // PATCH 실패 시 스냅샷은 변하지 않고 에러만 노출된다(낙관적 UI 금지).
            val result = repository.setSoldOut(menuItemId = "101", soldOut = true)
            assertTrue(result is AppResult.Failure)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setVisibleEmitsUpdatedSnapshotToActiveObserver() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus().test {
            assertTrue(awaitItem() is AppResult.Success)

            val result = repository.setVisible(menuItemId = "101", visible = false)
            assertTrue(result is AppResult.Success)

            val updated = awaitItem()
            assertTrue(updated is AppResult.Success)
            assertFalse((updated as AppResult.Success).data.first { it.id == "101" }.isVisible)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOwnerMenuRepository()

        val result = repository.setSoldOut(menuItemId = "101", soldOut = true)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun setVisibleIsLocalAndSkipsServer() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(menusResponse())
        val repository = realOwnerMenuRepository()

        repository.observeManagedMenus().test {
            assertTrue(awaitItem() is AppResult.Success)
            cancelAndIgnoreRemainingEvents()
        }

        val result = repository.setVisible(menuItemId = "101", visible = false)

        assertTrue(result is AppResult.Success)
        assertFalse((result as AppResult.Success).data.isVisible)
        // 가시성은 서버 엔드포인트가 없어 추가 네트워크 호출을 하지 않는다(관측 2건만).
        assertEquals(2, server.requestCount)
    }

    @Test
    fun addMenuPostsCreateAndReflectsServerId() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{ "menuId": 555 }"""))
        val repository = realOwnerMenuRepository()

        val draft = NewMenuDraft(
            name = "콜드브루",
            categoryId = "coffee",
            basePrice = 5_500,
            description = "깊고 진한 콜드브루",
            imageUrl = null,
            isSoldOut = false,
        )

        val result = repository.addMenu(draft)

        assertTrue(result is AppResult.Success)
        val created = (result as AppResult.Success).data
        assertEquals("555", created.id)
        assertEquals("콜드브루", created.name)
        assertEquals(5_500, created.basePrice)
        assertEquals("coffee", created.categoryId)

        server.takeRequest() // stores/my
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/stores/7/menus", request.requestUrl?.encodedPath)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"name\":\"콜드브루\""))
        assertTrue(body.contains("\"price\":5500"))
        assertTrue(body.contains("\"category\":\"coffee\""))
    }

    @Test
    fun addMenuUploadsLocalImageThenCreatesWithReturnedUrl() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        // 1) 이미지 업로드 응답: 서버가 원격 URL 을 돌려준다.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{ "imageUrl": "https://cdn.example.com/menu/cold-brew.jpg" }"""),
        )
        // 2) 메뉴 생성 응답.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{ "menuId": 777 }"""))
        val reader = FakeMenuImageReader(
            data = MenuImageData(bytes = byteArrayOf(1, 2, 3), mimeType = "image/png", fileName = "menu_image.png"),
        )
        val repository = realOwnerMenuRepository(imageReader = reader)

        val result = repository.addMenu(
            NewMenuDraft(
                name = "콜드브루",
                categoryId = "coffee",
                basePrice = 5_500,
                description = "",
                imageUrl = "content://media/external/images/42",
                isSoldOut = false,
            ),
        )

        assertTrue(result is AppResult.Success)
        val created = (result as AppResult.Success).data
        // 로컬 content:// 가 아니라 업로드로 받은 원격 URL 이 확정돼야 한다.
        assertEquals("https://cdn.example.com/menu/cold-brew.jpg", created.imageUrl)
        assertEquals("content://media/external/images/42", reader.lastReadUri)

        server.takeRequest() // stores/my
        val uploadRequest = server.takeRequest()
        assertEquals("POST", uploadRequest.method)
        assertEquals("/api/images/menu", uploadRequest.requestUrl?.encodedPath)
        assertTrue(uploadRequest.headers["Content-Type"]?.startsWith("multipart/form-data") == true)

        val createRequest = server.takeRequest()
        assertEquals("/api/stores/7/menus", createRequest.requestUrl?.encodedPath)
        val body = createRequest.body.readUtf8()
        assertTrue(body.contains("\"imageUrl\":\"https://cdn.example.com/menu/cold-brew.jpg\""))
        // 잘못된 content:// 가 생성 요청으로 새어 나가지 않아야 한다.
        assertFalse(body.contains("content://"))
    }

    @Test
    fun addMenuUploadFailureFailsWithoutCreatingMenu() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        // 업로드 실패 → 메뉴 생성으로 진행하지 않는다(낙관 금지).
        server.enqueue(MockResponse().setResponseCode(500))
        val reader = FakeMenuImageReader(
            data = MenuImageData(bytes = byteArrayOf(9), mimeType = "image/jpeg", fileName = "menu_image.jpg"),
        )
        val repository = realOwnerMenuRepository(imageReader = reader)

        val result = repository.addMenu(
            NewMenuDraft(
                name = "콜드브루",
                categoryId = "coffee",
                basePrice = 5_500,
                description = "",
                imageUrl = "content://media/external/images/42",
                isSoldOut = false,
            ),
        )

        assertTrue(result is AppResult.Failure)
        // stores/my + 업로드(실패) 까지만 호출되고 생성 호출은 없어야 한다.
        assertEquals(2, server.requestCount)
    }

    @Test
    fun addMenuUnreadableLocalImageProceedsWithoutImage() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{ "menuId": 888 }"""))
        // 리더가 null 을 반환(읽기 실패) → 업로드 없이 이미지 없이 생성한다.
        val reader = FakeMenuImageReader(data = null)
        val repository = realOwnerMenuRepository(imageReader = reader)

        val result = repository.addMenu(
            NewMenuDraft(
                name = "콜드브루",
                categoryId = "coffee",
                basePrice = 5_500,
                description = "",
                imageUrl = "content://media/external/images/42",
                isSoldOut = false,
            ),
        )

        assertTrue(result is AppResult.Success)
        assertEquals(null, (result as AppResult.Success).data.imageUrl)
        // 업로드 호출 없이 stores/my + create 만 발생.
        assertEquals(2, server.requestCount)
        server.takeRequest() // stores/my
        val createRequest = server.takeRequest()
        assertEquals("/api/stores/7/menus", createRequest.requestUrl?.encodedPath)
        assertFalse(createRequest.body.readUtf8().contains("content://"))
    }

    @Test
    fun addMenuHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOwnerMenuRepository()

        val result = repository.addMenu(
            NewMenuDraft(
                name = "콜드브루",
                categoryId = "coffee",
                basePrice = 5_500,
                description = "",
                imageUrl = null,
                isSoldOut = false,
            ),
        )

        assertTrue(result is AppResult.Failure)
    }

    private fun realOwnerMenuRepository(
        imageReader: MenuImageReader = FakeMenuImageReader(data = null),
    ): RealOwnerMenuRepository {
        val retrofit = retrofit()
        return RealOwnerMenuRepository(
            ownerMenuApi = retrofit.create(OwnerMenuApi::class.java),
            ownerOrderApi = retrofit.create(OwnerOrderApi::class.java),
            menuApi = retrofit.create(MenuApi::class.java),
            menuImageReader = imageReader,
            ioDispatcher = testDispatcher,
        )
    }

    private fun retrofit(): Retrofit =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        )

    private fun myStoresResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  { "id": 7, "name": "강남점", "imageUrl": null }
                ]
                """.trimIndent(),
            )

    private fun menusResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  { "id": 101, "name": "아메리카노", "price": 4500, "category": "coffee", "imageUrl": null, "isAvailable": true },
                  { "id": 102, "name": "바닐라라떼", "price": 5500, "category": "coffee", "imageUrl": null, "isAvailable": false }
                ]
                """.trimIndent(),
            )
}

private class FakeMenuImageReader(
    private val data: MenuImageData?,
) : MenuImageReader {
    var lastReadUri: String? = null
        private set

    override suspend fun read(localUri: String): MenuImageData? {
        lastReadUri = localUri
        return data
    }
}
