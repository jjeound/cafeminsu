package com.cafeminsu.ui.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsActiveOrderPastOrdersAndReorderAction() {
        var reorderedOrderId: String? = null

        composeRule.setContent {
            CafeTheme {
                HistoryScreen(
                    state = HistoryUiState.Content(
                        activeOrder = HistoryActiveOrderUiModel(
                            id = "active-1",
                            orderNumber = "#A-2419",
                            itemSummary = "바닐라라떼 외 1개",
                            amountLabel = "10,000원",
                            steps = historyOrderSteps(com.cafeminsu.domain.model.OrderStatus.Preparing),
                        ),
                        pastOrders = listOf(
                            HistoryPastOrderUiModel(
                                id = "past-1",
                                storeName = "강남역",
                                dateLabel = "어제 14:32",
                                itemSummary = "아메리카노 ✕ 2",
                                amountLabel = "9,000원",
                                reorderMenuItemId = "americano",
                            ),
                        ),
                    ),
                    onBackClick = {},
                    onRetry = {},
                    onReorderClick = { orderId -> reorderedOrderId = orderId },
                )
            }
        }

        composeRule.onNodeWithText("주문내역").assertIsDisplayed()
        composeRule.onNodeWithText("진행중인 주문").assertIsDisplayed()
        composeRule.onNodeWithText("#A-2419").assertIsDisplayed()
        composeRule.onNodeWithText("접수").assertIsDisplayed()
        composeRule.onNodeWithText("수락").assertIsDisplayed()
        composeRule.onNodeWithText("준비중").assertIsDisplayed()
        composeRule.onNodeWithText("완료").assertIsDisplayed()
        composeRule.onNodeWithText("지난 주문").assertIsDisplayed()
        composeRule.onNodeWithText("강남역").assertIsDisplayed()
        composeRule.onNodeWithText("9,000원").assertIsDisplayed()
        composeRule.onNodeWithText("↻ 재주문").performClick()

        assertEquals("past-1", reorderedOrderId)
    }

    @Test
    fun showsEmptyHistoryState() {
        composeRule.setContent {
            CafeTheme {
                HistoryScreen(
                    state = HistoryUiState.Empty(
                        title = "아직 주문 내역이 없어요",
                        message = "첫 번째 한 잔을 주문해보세요",
                    ),
                    onBackClick = {},
                    onRetry = {},
                    onReorderClick = {},
                )
            }
        }

        composeRule.onNodeWithText("주문내역").assertIsDisplayed()
        composeRule.onNodeWithText("아직 주문 내역이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("첫 번째 한 잔을 주문해보세요").assertIsDisplayed()
    }
}
