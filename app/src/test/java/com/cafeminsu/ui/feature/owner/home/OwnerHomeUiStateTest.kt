package com.cafeminsu.ui.feature.owner.home

import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerHomeUiStateTest {
    @Test
    fun statsKeepRawDashboardValues() {
        val stats = OwnerHomeStatsUiModel(
            totalSales = 482_000,
            orderCount = 37,
            newWaitingCount = 3,
        )

        assertEquals(482_000, stats.totalSales)
        assertEquals(37, stats.orderCount)
        assertEquals(3, stats.newWaitingCount)
    }

    @Test
    fun orderSummaryKeepsNextOperationalAction() {
        val order = OwnerHomeOrderUiModel(
            id = "owner-order-1042",
            orderNumberLabel = "#1042",
            timeLabel = "오후 2:14",
            status = OrderStatus.Accepted,
            statusLabel = "신규",
            itemSummary = "아메리카노(L) ICE 외 1",
            totalAmount = 9_300,
            actionLabel = "접수하기",
            isActionInProgress = false,
        )

        assertEquals(OrderStatus.Accepted, order.status)
        assertEquals("접수하기", order.actionLabel)
    }

    @Test
    fun storeUiModelMarksSelectedStore() {
        val stores = listOf(
            OwnerStoreUiModel(id = "store-gangnam", name = "강남점", isSelected = true),
            OwnerStoreUiModel(id = "store-hongdae", name = "홍대점", isSelected = false),
        )

        assertEquals("강남점", stores.single { it.isSelected }.name)
        assertEquals(2, stores.size)
    }
}
