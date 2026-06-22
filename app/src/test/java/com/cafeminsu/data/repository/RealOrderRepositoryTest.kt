package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.local.order.OrderHistoryLocalDataSource
import com.cafeminsu.data.remote.OrderApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
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

class RealOrderRepositoryTest {
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
    fun createOrderFromCartPostsLocalCartAndUsesServerConfirmedAmount() = runTest(testDispatcher) {
        server.enqueue(orderCreateResponse(totalAmount = 13_000))
        val repository = realOrderRepository()

        val result = repository.createOrderFromCart(sampleCart(subtotal = 12_000))

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data
        assertEquals("77", order.id)
        assertEquals("A-2543", order.orderNumber)
        assertEquals(13_000, order.totalAmount)
        assertEquals(OrderStatus.PendingPayment, order.status)
        assertEquals(sampleCartItems(), order.items)

        val request = server.takeRequest()
        assertEquals("/api/orders", request.requestUrl?.encodedPath)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"storeId\":11"))
        assertTrue(body.contains("\"orderType\":\"MOBILE\""))
        assertTrue(body.contains("\"orderMethod\":\"MANUAL\""))
        assertTrue(body.contains("\"menuId\":101"))
        assertTrue(body.contains("\"quantity\":2"))
        assertTrue(body.contains("\"optionIds\":[1,2]"))
        assertFalse(body.contains("requestNote"))
    }

    @Test
    fun observeOrderFetchesDetailAndMapsStatusAndItems() = runTest(testDispatcher) {
        server.enqueue(orderDetailResponse())
        val repository = realOrderRepository()

        repository.observeOrder("77").test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val order = (result as AppResult.Success).data
            assertEquals("77", order.id)
            assertEquals(OrderStatus.Ready, order.status)
            assertEquals(10_000, order.totalAmount)
            assertEquals("101", order.items.single().menuItemId)
            assertEquals("바닐라라떼", order.items.single().name)
            assertEquals("1", order.items.single().selectedOptions.single().optionId)
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/orders/77", request.requestUrl?.encodedPath)
    }

    @Test
    fun observeOrderHistoryFetchesMyOrdersPageAndMapsList() = runTest(testDispatcher) {
        server.enqueue(orderHistoryResponse())
        val repository = realOrderRepository()

        repository.observeOrderHistory().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val orders = (result as AppResult.Success).data
            assertEquals(listOf("77", "78"), orders.map { it.id })
            assertEquals(listOf(OrderStatus.Completed, OrderStatus.Accepted), orders.map { it.status })
            assertTrue(orders.all { it.items.isEmpty() })
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/orders/my", request.requestUrl?.encodedPath)
        assertEquals("0", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("size"))
    }

    @Test
    fun serverHttpErrorMapsToAppResultFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realOrderRepository()

        repository.observeOrder("404").test {
            assertEquals(AppResult.Failure(DomainError.NotFound), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unauthenticatedSessionBlocksWritesBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realOrderRepository(authState = AuthState.Guest)

        val result = repository.createOrderFromCart(sampleCart())

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun observeOrderHistoryWritesThroughToCacheOnSuccess() = runTest(testDispatcher) {
        server.enqueue(orderHistoryResponse())
        val cache = FakeOrderHistoryLocalDataSource()
        val repository = realOrderRepository(local = cache)

        repository.observeOrderHistory().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, cache.replaceCount)
        assertEquals(listOf("77", "78"), cache.stored().map { it.id })
    }

    @Test
    fun observeOrderHistoryFallsBackToCacheOnNetworkFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val cache = FakeOrderHistoryLocalDataSource(initial = cachedOrders())
        val repository = realOrderRepository(local = cache)

        repository.observeOrderHistory().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(listOf("91", "92"), (result as AppResult.Success).data.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeOrderHistoryEmitsFailureWhenNetworkFailsAndCacheEmpty() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOrderRepository(local = FakeOrderHistoryLocalDataSource())

        repository.observeOrderHistory().test {
            assertTrue(awaitItem() is AppResult.Failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeOrderHistoryGuestSessionBlocksCacheAndNetwork() = runTest(testDispatcher) {
        val cache = FakeOrderHistoryLocalDataSource(initial = cachedOrders())
        val repository = realOrderRepository(authState = AuthState.Guest, local = cache)

        repository.observeOrderHistory().test {
            assertEquals(AppResult.Failure(DomainError.Unauthorized), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // 미인증이면 네트워크는 물론 캐시도 읽지 않는다(다른 사용자 데이터 누출 방지).
        assertEquals(0, server.requestCount)
        assertEquals(0, cache.cachedCount)
        assertEquals(0, cache.replaceCount)
    }

    @Test
    fun observeOrderDoesNotTouchHistoryCache() = runTest(testDispatcher) {
        // 주문 단건 조회는 캐시 경로(금전/실시간 상태)와 무관하다.
        server.enqueue(orderDetailResponse())
        val cache = FakeOrderHistoryLocalDataSource(initial = cachedOrders())
        val repository = realOrderRepository(local = cache)

        repository.observeOrder("77").test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, cache.cachedCount)
        assertEquals(0, cache.replaceCount)
    }

    @Test
    fun createOrderFromCartDoesNotTouchHistoryCache() = runTest(testDispatcher) {
        // 주문 생성은 금전 액션이라 캐시/낙관 적용 금지.
        server.enqueue(orderCreateResponse(totalAmount = 13_000))
        val cache = FakeOrderHistoryLocalDataSource()
        val repository = realOrderRepository(local = cache)

        repository.createOrderFromCart(sampleCart())

        assertEquals(0, cache.cachedCount)
        assertEquals(0, cache.replaceCount)
    }

    private fun realOrderRepository(
        authState: AuthState = authenticatedState(),
        local: OrderHistoryLocalDataSource = FakeOrderHistoryLocalDataSource(),
    ): RealOrderRepository {
        val holder = selectedStoreHolderForTest()
        holder.select(sampleStore())
        return RealOrderRepository(
            orderApi = orderApi(),
            selectedStoreHolder = holder,
            sessionStateHolder = SessionStateHolder(authState),
            localDataSource = local,
            ioDispatcher = testDispatcher,
        )
    }

    private fun cachedOrders(): List<Order> =
        listOf(
            Order(
                id = "91",
                orderNumber = "A-9100",
                items = emptyList(),
                totalAmount = 10_000,
                status = OrderStatus.Completed,
                createdAtMillis = 20L,
            ),
            Order(
                id = "92",
                orderNumber = "A-9200",
                items = emptyList(),
                totalAmount = 8_500,
                status = OrderStatus.Accepted,
                createdAtMillis = 10L,
            ),
        )

    private fun orderApi(): OrderApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(OrderApi::class.java)

    private fun orderCreateResponse(totalAmount: Int): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "orderId": 77,
                  "orderNumber": "A-2543",
                  "totalAmount": $totalAmount,
                  "status": "PENDING"
                }
                """.trimIndent(),
            )

    private fun orderDetailResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "orderId": 77,
                  "orderNumber": "A-2543",
                  "storeId": 11,
                  "storeName": "카페민수 강남점",
                  "orderType": "MOBILE",
                  "orderMethod": "MANUAL",
                  "status": "READY",
                  "totalAmount": 10000,
                  "cancelReason": null,
                  "items": [
                    {
                      "menuId": 101,
                      "menuName": "바닐라라떼",
                      "quantity": 1,
                      "unitPrice": 5500,
                      "options": [
                        {
                          "optionId": 1,
                          "optionGroup": "온도",
                          "optionName": "ICE",
                          "optionPrice": 0
                        }
                      ],
                      "subtotal": 5500
                    }
                  ],
                  "payment": null,
                  "createdAt": "2026-06-20T01:15:30Z"
                }
                """.trimIndent(),
            )

    private fun orderHistoryResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "orderId": 77,
                    "orderNumber": "A-2543",
                    "storeName": "카페민수 강남점",
                    "totalAmount": 10000,
                    "status": "DONE",
                    "createdAt": "2026-06-20T01:15:30Z"
                  },
                  {
                    "orderId": 78,
                    "orderNumber": "A-2544",
                    "storeName": "카페민수 강남점",
                    "totalAmount": 8500,
                    "status": "ACCEPTED",
                    "createdAt": "2026-06-20T01:20:30Z"
                  }
                ]
                """.trimIndent(),
            )

    private fun sampleCart(subtotal: Int = 12_000): Cart =
        Cart(
            items = sampleCartItems(),
            subtotal = subtotal,
            validation = CartValidation.Valid,
        )

    private fun sampleCartItems(): List<CartItem> =
        listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "101",
                name = "바닐라라떼",
                unitPrice = 6_000,
                selectedOptions = listOf(
                    SelectedOption(
                        groupId = "온도",
                        optionId = "1",
                        name = "ICE",
                        extraPrice = 0,
                    ),
                    SelectedOption(
                        groupId = "샷 추가",
                        optionId = "2",
                        name = "+1샷",
                        extraPrice = 500,
                    ),
                ),
                quantity = 2,
            ),
        )

    private fun sampleStore(): Store =
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

private class FakeOrderHistoryLocalDataSource(
    initial: List<Order> = emptyList(),
) : OrderHistoryLocalDataSource {
    private var store: List<Order> = initial
    var replaceCount: Int = 0
        private set
    var cachedCount: Int = 0
        private set

    override suspend fun cachedHistory(): List<Order> {
        cachedCount++
        return store
    }

    override suspend fun replaceHistory(orders: List<Order>) {
        store = orders
        replaceCount++
    }

    fun stored(): List<Order> = store
}
