package com.cafeminsu.ui.feature.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.StampEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderResultUiStateTest {
    @Test
    fun mapsOrderAndStampCardToSuccessSummary() {
        val summary = sampleOrder().toOrderSuccessSummary(stampCard = sampleStampCard())

        assertEquals("order-1", summary.orderId)
        assertEquals("A-2543", summary.orderNumber)
        assertEquals("카페민수 강남점", summary.pickupStoreName)
        assertEquals("약 8분 후", summary.estimatedReadyLabel)
        assertEquals("8,500원", summary.paidAmountLabel)
        assertEquals("스탬프 1개가 적립됐어요 (8/10)", summary.stampMessage)
    }
}

private fun sampleOrder(): Order =
    Order(
        id = "order-1",
        orderNumber = "A-2543",
        items = listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "latte",
                name = "바닐라라떼",
                unitPrice = 8_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 8_500,
        status = OrderStatus.Paid,
        createdAtMillis = 1_800_000_000_000L,
    )

private fun sampleStampCard(): StampCard =
    StampCard(
        userId = "user-1",
        currentCount = 8,
        goalCount = 10,
        history = listOf(
            StampEvent(
                id = "stamp-1",
                orderId = "order-1",
                count = 1,
                createdAtMillis = 1_800_000_000_000L,
            ),
        ),
    )
