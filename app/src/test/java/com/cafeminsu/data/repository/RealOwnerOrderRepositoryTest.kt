package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.OrderStatus
import java.time.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealOwnerOrderRepositoryTest {
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
    fun observeIncomingOrdersResolvesStoreThenMapsOrders() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(storeOrdersResponse())
        val repository = realOwnerOrderRepository()

        repository.observeIncomingOrders().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            val orders = (result as AppResult.Success).data
            assertEquals(listOf("1042", "1041"), orders.map { it.id })
            // 서버 PENDING=신규(Accepted), ACCEPTED=준비중(Preparing) 으로 매핑된다.
            assertEquals(listOf(OrderStatus.Accepted, OrderStatus.Preparing), orders.map { it.status })

            val first = orders.first()
            assertEquals("1042", first.orderNumber)
            assertEquals(9_300, first.totalAmount)
            assertEquals(Instant.parse("2026-06-20T01:15:30Z").toEpochMilli(), first.createdAtMillis)
            assertEquals(listOf("1042-101", "1042-102"), first.items.map { it.id })
            assertEquals("아메리카노", first.items.first().name)
            assertEquals(2, first.items.first().quantity)
            cancelAndIgnoreRemainingEvents()
        }

        val storesRequest = server.takeRequest()
        assertEquals("/api/stores/my", storesRequest.requestUrl?.encodedPath)
        val ordersRequest = server.takeRequest()
        assertEquals("/api/stores/7/orders", ordersRequest.requestUrl?.encodedPath)
        // 점주 화면은 필터 없이 전체를 조회하고 클라이언트에서 탭별로 거른다.
        assertEquals(null, ordersRequest.requestUrl?.queryParameter("status"))
    }

    @Test
    fun selectingStoreReloadsOrdersForThatStore() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse()) // 초기 holder=null → stores/my 로 첫 매장(7) 해석
        server.enqueue(storeOrdersResponse()) // store 7 주문
        server.enqueue(store9OrdersResponse()) // 매장 전환 후 store 9 주문
        val holder = OwnerSelectedStoreHolder()
        val repository = realOwnerOrderRepository(selectedStoreHolder = holder)

        repository.observeIncomingOrders().test {
            val initial = (awaitItem() as AppResult.Success).data
            assertEquals(listOf("1042", "1041"), initial.map { it.id })

            // 점주가 다른 매장을 선택하면 그 매장 기준으로 즉시 다시 로드해야 한다.
            holder.select("9")
            val switched = (awaitItem() as AppResult.Success).data
            assertEquals(listOf("2001"), switched.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("/api/stores/my", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/api/stores/7/orders", server.takeRequest().requestUrl?.encodedPath)
        // 선택 매장이 있으면 stores/my 를 다시 거치지 않고 곧장 그 매장 주문을 조회한다.
        assertEquals("/api/stores/9/orders", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun emptyMyStoresReturnsEmptyListWithoutOrdersCall() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repository = realOwnerOrderRepository()

        repository.observeIncomingOrders().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertTrue((result as AppResult.Success).data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // stores/my 가 비어 있으면 주문 호출 없이 안전하게 빈 목록을 낸다.
        assertEquals(1, server.requestCount)
        assertEquals("/api/stores/my", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun storeOrdersHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realOwnerOrderRepository()

        repository.observeIncomingOrders().test {
            assertTrue(awaitItem() is AppResult.Failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun advanceStatusPreparingCallsAcceptEndpointAndConfirmsPreparing() = runTest(testDispatcher) {
        // "접수하기" = 신규 → 준비중. 서버 accept 엔드포인트(ACCEPTED)를 호출하고 준비중으로 확정.
        server.enqueue(orderStatusResponse("ACCEPTED"))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Preparing)

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data
        assertEquals("1042", order.id)
        assertEquals(OrderStatus.Preparing, order.status)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/orders/1042/accept", request.requestUrl?.encodedPath)
    }

    @Test
    fun advanceStatusReadyCallsReadyEndpoint() = runTest(testDispatcher) {
        server.enqueue(orderStatusResponse("READY"))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Ready)

        assertTrue(result is AppResult.Success)
        assertEquals(OrderStatus.Ready, (result as AppResult.Success).data.status)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/orders/1042/ready", request.requestUrl?.encodedPath)
    }

    @Test
    fun advanceStatusCompletedCallsCompleteEndpoint() = runTest(testDispatcher) {
        server.enqueue(orderStatusResponse("DONE"))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Completed)

        assertTrue(result is AppResult.Success)
        assertEquals(OrderStatus.Completed, (result as AppResult.Success).data.status)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/orders/1042/complete", request.requestUrl?.encodedPath)
    }

    @Test
    fun advanceStatusCancelledPostsCancelWithReasonBody() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Cancelled)

        assertTrue(result is AppResult.Success)
        assertEquals(OrderStatus.Cancelled, (result as AppResult.Success).data.status)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/orders/1042/cancel", request.requestUrl?.encodedPath)
        assertTrue(request.body.readUtf8().contains("\"reason\""))
    }

    @Test
    fun advanceStatusHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Preparing)

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun advanceStatusReflectsInObservedOrders() = runTest(testDispatcher) {
        server.enqueue(myStoresResponse())
        server.enqueue(storeOrdersResponse())
        server.enqueue(orderStatusResponse("ACCEPTED")) // 접수 endpoint 응답
        val repository = realOwnerOrderRepository()

        repository.observeIncomingOrders().test {
            val initial = (awaitItem() as AppResult.Success).data
            // 접수 전: 신규(Accepted) 주문이 큐에 있다.
            assertEquals(OrderStatus.Accepted, initial.first { it.id == "1042" }.status)

            // "접수하기" = 신규 → 준비중. 서버 accept 호출 후 관찰 중인 목록이 즉시 갱신돼야 한다.
            repository.advanceStatus("1042", OrderStatus.Preparing)
            val updated = (awaitItem() as AppResult.Success).data
            val advanced = updated.first { it.id == "1042" }
            assertEquals(OrderStatus.Preparing, advanced.status)
            // 항목/번호/금액 등 기존 정보는 보존된다(전이로 사라지지 않음).
            assertEquals("1042", advanced.orderNumber)
            assertEquals(9_300, advanced.totalAmount)

            cancelAndIgnoreRemainingEvents()
        }

        // 접수 시 실제로 /api/orders/{id}/accept 가 호출됐는지 확인한다.
        server.takeRequest() // stores/my
        server.takeRequest() // stores/{id}/orders
        val acceptRequest = server.takeRequest()
        assertEquals("PATCH", acceptRequest.method)
        assertEquals("/api/orders/1042/accept", acceptRequest.requestUrl?.encodedPath)
    }

    private fun realOwnerOrderRepository(
        selectedStoreHolder: OwnerSelectedStoreHolder = OwnerSelectedStoreHolder(),
    ): RealOwnerOrderRepository =
        RealOwnerOrderRepository(
            ownerOrderApi = ownerOrderApi(),
            ownerSelectedStoreHolder = selectedStoreHolder,
            ioDispatcher = testDispatcher,
        )

    private fun ownerOrderApi(): OwnerOrderApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(OwnerOrderApi::class.java)

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

    private fun storeOrdersResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "orderId": 1042,
                    "orderNumber": "1042",
                    "status": "PENDING",
                    "totalAmount": 9300,
                    "items": [
                      { "menuId": 101, "menuName": "아메리카노", "quantity": 2 },
                      { "menuId": 102, "menuName": "바닐라라떼", "quantity": 1 }
                    ],
                    "createdAt": "2026-06-20T01:15:30Z"
                  },
                  {
                    "orderId": 1041,
                    "orderNumber": "1041",
                    "status": "ACCEPTED",
                    "totalAmount": 11000,
                    "items": [
                      { "menuId": 103, "menuName": "카페라떼", "quantity": 1 }
                    ],
                    "createdAt": "2026-06-20T01:10:00Z"
                  }
                ]
                """.trimIndent(),
            )

    private fun store9OrdersResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "orderId": 2001,
                    "orderNumber": "2001",
                    "status": "PENDING",
                    "totalAmount": 4500,
                    "items": [
                      { "menuId": 201, "menuName": "콜드브루", "quantity": 1 }
                    ],
                    "createdAt": "2026-06-20T02:00:00Z"
                  }
                ]
                """.trimIndent(),
            )

    private fun orderStatusResponse(status: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{ "status": "$status" }""")
}
