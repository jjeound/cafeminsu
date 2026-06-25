package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.model.order.OrderStatus
import com.ssafy.cafeminsu.core.network.model.response.order.OrderSummaryResponse
import com.ssafy.cafeminsu.core.network.model.response.order.OwnerOrderSummaryResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderResponseMapperTest {
    @Test
    fun `maps customer order summary response to domain summary`() {
        val response = OrderSummaryResponse(
            orderId = 1L,
            orderNumber = "A-001",
            storeName = "민수 카페",
            totalAmount = 4_500,
            status = "ACCEPTED",
            createdAt = "2026-06-24T00:00:00Z",
        )

        val order = response.asExternalModel()

        assertEquals(1L, order.id)
        assertEquals("A-001", order.orderNumber)
        assertEquals(4_500, order.totalAmount)
        assertEquals(OrderStatus.Accepted, order.status)
        assertEquals(1_782_259_200_000L, order.createdAtMillis)
    }

    @Test
    fun `maps owner order summary response to domain summary`() {
        val response = OwnerOrderSummaryResponse(
            orderId = 2L,
            orderNumber = "B-002",
            status = "READY",
            totalAmount = 5_000,
            items = emptyList(),
            createdAt = "2026-06-24T00:00:00Z",
        )

        val order = response.asExternalModel()

        assertEquals(2L, order.id)
        assertEquals(OrderStatus.Ready, order.status)
    }
}
