package com.cafeminsu.ui.feature.owner.sales

import com.cafeminsu.domain.model.SalesPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerSalesUiStateTest {
    @Test
    fun contentHoldsSelectedPeriodAndSalesSummaryUiModels() {
        val state = OwnerSalesUiState.Content(
            selectedPeriod = SalesPeriod.Week,
            periods = SalesPeriod.entries.map { period ->
                OwnerSalesPeriodUiModel(
                    period = period,
                    label = period.ownerSalesLabel,
                    selected = period == SalesPeriod.Week,
                )
            },
            summary = OwnerSalesSummaryUiModel(
                periodSalesLabel = "이번 주 매출",
                totalSalesLabel = "₩2,840,000",
                deltaLabel = "▲ 12% 지난주 대비",
                deltaTone = OwnerSalesDeltaTone.Positive,
                bars = listOf(
                    OwnerSalesBarUiModel(label = "금", ratio = 1f, highlighted = true),
                ),
                topMenus = listOf(
                    OwnerSalesTopMenuUiModel(
                        rankLabel = "1",
                        name = "아메리카노",
                        soldCountLabel = "142잔",
                        salesLabel = "₩639,000",
                    ),
                ),
                payoutAmountLabel = "₩2,556,000",
                payoutDateLabel = "6월 24일 입금 예정",
            ),
        )

        assertEquals(SalesPeriod.Week, state.selectedPeriod)
        assertEquals("이번 주", state.periods.first { it.selected }.label)
        assertTrue(state.summary.bars.first().highlighted)
    }
}
