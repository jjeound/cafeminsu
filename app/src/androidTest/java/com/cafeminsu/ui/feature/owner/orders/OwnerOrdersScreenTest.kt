package com.cafeminsu.ui.feature.owner.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.scheduling.SchedulingBadge
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerOrdersScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOwnerOrdersAndHandlesFilterAndActionClicks() {
        var selectedFilter: OwnerOrdersFilter? = null
        var advancedOrderId: String? = null

        composeRule.setContent {
            CafeTheme {
                OwnerOrdersScreen(
                    state = OwnerOrdersUiState.Content(
                        selectedFilter = OwnerOrdersFilter.New,
                        counts = OwnerOrdersCountsUiModel(
                            newCount = 3,
                            preparingCount = 5,
                            readyCount = 2,
                        ),
                        orders = listOf(
                            OwnerOrdersOrderUiModel(
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
                                priorityBadge = SchedulingBadge.Urgent,
                                etaLabel = "약 4분",
                            ),
                        ),
                    ),
                    onFilterSelected = { selectedFilter = it },
                    onAdvanceStatus = { advancedOrderId = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("주문 관리").assertIsDisplayed()
        composeRule.onNodeWithText("실시간").assertIsDisplayed()
        composeRule.onNodeWithText("신규 3").assertIsDisplayed()
        composeRule.onNodeWithText("준비중 5").assertIsDisplayed()
        composeRule.onNodeWithText("준비완료 2").assertIsDisplayed()
        composeRule.onNodeWithText("#1042").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노 (L) · ICE · 1\n바닐라라떼 (R) · HOT · 1").assertIsDisplayed()
        composeRule.onNodeWithText("포장 · 요청: 얼음 적게").assertIsDisplayed()
        composeRule.onNodeWithText("₩9,300").assertIsDisplayed()
        composeRule.onNodeWithText("긴급").assertIsDisplayed()
        composeRule.onNodeWithText("약 4분").assertIsDisplayed()

        composeRule.onNodeWithText("준비중 5").performClick()
        composeRule.onNodeWithText("접수하기").performClick()

        assertEquals(OwnerOrdersFilter.Preparing, selectedFilter)
        assertEquals("owner-order-1042", advancedOrderId)
    }
}
