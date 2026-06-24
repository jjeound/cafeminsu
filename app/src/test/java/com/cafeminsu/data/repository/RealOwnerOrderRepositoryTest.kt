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

        repository.observeIncomingOrders(OrderStatus.Accepted).test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            val orders = (result as AppResult.Success).data
            assertEquals(listOf("1042", "1041"), orders.map { it.id })
            assertEquals(listOf(OrderStatus.Accepted, OrderStatus.Accepted), orders.map { it.status })

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
        assertEquals("ACCEPTED", ordersRequest.requestUrl?.queryParameter("status"))
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
    fun advanceStatusAcceptedCallsAcceptEndpointAndConfirmsStatus() = runTest(testDispatcher) {
        server.enqueue(orderStatusResponse("ACCEPTED"))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Accepted)

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data
        assertEquals("1042", order.id)
        assertEquals(OrderStatus.Accepted, order.status)

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
    fun advanceStatusPreparingIsLocalAndSkipsServer() = runTest(testDispatcher) {
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Preparing)

        assertTrue(result is AppResult.Success)
        assertEquals(OrderStatus.Preparing, (result as AppResult.Success).data.status)
        // 서버에 대응 엔드포인트가 없는 Preparing 은 네트워크 호출을 하지 않는다.
        assertEquals(0, server.requestCount)
    }

    @Test
    fun advanceStatusHttpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realOwnerOrderRepository()

        val result = repository.advanceStatus("1042", OrderStatus.Accepted)

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    private fun realOwnerOrderRepository(): RealOwnerOrderRepository =
        RealOwnerOrderRepository(
            ownerOrderApi = ownerOrderApi(),
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

    private fun orderStatusResponse(status: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{ "status": "$status" }""")
}
