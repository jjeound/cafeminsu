package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SalesTest {
    @Test
    fun salesPeriodOrderMatchesOwnerSalesSegment() {
        assertEquals(
            listOf(SalesPeriod.Today, SalesPeriod.Week, SalesPeriod.Month),
            SalesPeriod.entries,
        )
    }

    @Test
    fun salesSummaryKeepsWonAmountsAndTopMenuRanking() {
        val summary = SalesSummary(
            period = SalesPeriod.Week,
            totalSales = 2_840_000,
            orderCount = 214,
            deltaPercent = 12,
            dailySales = listOf(300_000, 400_000, 500_000),
            topMenus = listOf(
                TopMenu(rank = 1, name = "아메리카노", soldCount = 142, sales = 639_000),
            ),
            payoutAmount = 2_556_000,
            payoutDateLabel = "6월 24일 입금 예정",
        )

        assertEquals(SalesPeriod.Week, summary.period)
        assertEquals(2_840_000, summary.totalSales)
        assertEquals(639_000, summary.topMenus.first().sales)
    }
}
