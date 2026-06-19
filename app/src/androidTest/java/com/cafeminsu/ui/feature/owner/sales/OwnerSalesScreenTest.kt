package com.cafeminsu.ui.feature.owner.sales

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerSalesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOwnerSalesContentAndHandlesPeriodSelection() {
        var selectedPeriod: SalesPeriod? = null

        composeRule.setContent {
            CafeTheme {
                OwnerSalesScreen(
                    state = OwnerSalesUiState.Content(
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
                                OwnerSalesBarUiModel("일", 0.45f, highlighted = false),
                                OwnerSalesBarUiModel("월", 0.72f, highlighted = false),
                                OwnerSalesBarUiModel("화", 0.39f, highlighted = false),
                                OwnerSalesBarUiModel("수", 0.88f, highlighted = false),
                                OwnerSalesBarUiModel("목", 0.64f, highlighted = false),
                                OwnerSalesBarUiModel("금", 1f, highlighted = true),
                                OwnerSalesBarUiModel("토", 0.91f, highlighted = false),
                            ),
                            topMenus = listOf(
                                OwnerSalesTopMenuUiModel("1", "아메리카노", "142잔", "₩639,000"),
                                OwnerSalesTopMenuUiModel("2", "카페라떼", "98잔", "₩490,000"),
                                OwnerSalesTopMenuUiModel("3", "바닐라라떼", "61잔", "₩335,500"),
                            ),
                            payoutAmountLabel = "₩2,556,000",
                            payoutDateLabel = "6월 24일 입금 예정",
                        ),
                    ),
                    onPeriodSelected = { selectedPeriod = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("매출 · 정산").assertIsDisplayed()
        composeRule.onNodeWithText("오늘").assertIsDisplayed()
        composeRule.onNodeWithText("이번 주").assertIsDisplayed()
        composeRule.onNodeWithText("이번 달").assertIsDisplayed()
        composeRule.onNodeWithText("이번 주 매출").assertIsDisplayed()
        composeRule.onNodeWithText("₩2,840,000").assertIsDisplayed()
        composeRule.onNodeWithText("▲ 12% 지난주 대비").assertIsDisplayed()
        composeRule.onNodeWithText("요일별 매출").assertIsDisplayed()
        composeRule.onNodeWithText("인기 메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("142잔").assertIsDisplayed()
        composeRule.onNodeWithText("₩639,000").assertIsDisplayed()
        composeRule.onNodeWithText("정산 예정 금액").assertIsDisplayed()
        composeRule.onNodeWithText("6월 24일 입금 예정").assertIsDisplayed()
        composeRule.onNodeWithText("₩2,556,000").assertIsDisplayed()

        composeRule.onNodeWithText("오늘").performClick()

        assertEquals(SalesPeriod.Today, selectedPeriod)
    }
}
