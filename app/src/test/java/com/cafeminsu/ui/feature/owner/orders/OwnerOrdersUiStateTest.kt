package com.cafeminsu.ui.feature.owner.orders

import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerOrdersUiStateTest {
    @Test
    fun countModelKeepsOperationalCounts() {
        val counts = OwnerOrdersCountsUiModel(
            newCount = 3,
            preparingCount = 5,
            readyCount = 2,
        )

        assertEquals(3, counts.newCount)
        assertEquals(5, counts.preparingCount)
        assertEquals(2, counts.readyCount)
    }

    @Test
    fun orderModelKeepsCurrentStatusAndNextAction() {
        val order = OwnerOrdersOrderUiModel(
            id = "owner-order-1042",
            orderNumberLabel = "#1042",
            timeLabel = "오후 2:14",
            status = OrderStatus.Accepted,
            statusLabel = "신규",
            itemsLabel = "아메리카노 (L) · ICE · 1\n바닐라라떼 (R) · HOT · 1",
            requestLabel = "포장 · 요청: 얼음 적게",
            totalAmount = 9_300,
            actionLabel = "접수하기",
            isActionInProgress = false,
        )

        assertEquals(OrderStatus.Accepted, order.status)
        assertEquals("접수하기", order.actionLabel)
        assertEquals("포장 · 요청: 얼음 적게", order.requestLabel)
    }
}
