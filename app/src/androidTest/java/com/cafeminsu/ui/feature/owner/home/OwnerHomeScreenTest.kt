package com.cafeminsu.ui.feature.owner.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerStore
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOwnerDashboardContentAndHandlesActions() {
        var viewAllClicked = false
        var toggledOpen: Boolean? = null
        var advancedOrderId: String? = null
        var selectedStoreId: String? = null

        composeRule.setContent {
            CafeTheme {
                OwnerHomeScreen(
                    state = OwnerHomeUiState.Content(
                        storeName = "강남점",
                        isStoreOpen = true,
                        dateLabel = "6월 19일 (금)",
                        stats = OwnerHomeStatsUiModel(
                            totalSales = 482_000,
                            orderCount = 37,
                            newWaitingCount = 3,
                        ),
                        pendingOrders = listOf(
                            OwnerHomeOrderUiModel(
                                id = "owner-order-1042",
                                orderNumberLabel = "#1042",
                                timeLabel = "오후 2:14",
                                status = OrderStatus.Accepted,
                                statusLabel = "신규",
                                itemSummary = "아메리카노(L) ICE 외 1",
                                totalAmount = 9_300,
                                actionLabel = "접수하기",
                                isActionInProgress = false,
                            ),
                        ),
                        isStoreOpenUpdating = false,
                        stores = listOf(
                            OwnerStore(id = "7", name = "강남점"),
                            OwnerStore(id = "8", name = "판교점"),
                        ),
                        selectedStoreId = "7",
                    ),
                    onToggleStoreOpen = { toggledOpen = it },
                    onAdvanceStatus = { advancedOrderId = it },
                    onViewAllOrders = { viewAllClicked = true },
                    onRetry = {},
                    onSelectStore = { selectedStoreId = it },
                )
            }
        }

        // 매장이 2개 이상이라 헤더는 매장 선택 드롭다운(이름 + ▾)으로 표시된다.
        composeRule.onNodeWithText("강남점 ▾").assertIsDisplayed()
        composeRule.onNodeWithText("영업중").assertIsDisplayed()
        composeRule.onNodeWithText("오늘의 매장 현황").assertIsDisplayed()
        composeRule.onNodeWithText("₩482,000").assertIsDisplayed()
        composeRule.onNodeWithText("37건").assertIsDisplayed()
        composeRule.onNodeWithText("3건").assertIsDisplayed()
        composeRule.onNodeWithText("지금 처리할 주문").assertIsDisplayed()
        composeRule.onNodeWithText("#1042").assertIsDisplayed()

        composeRule.onNodeWithText("전체 보기 →").performClick()
        composeRule.onNodeWithText("접수하기").performClick()
        composeRule.onNodeWithText("영업중").performClick()

        assertEquals(true, viewAllClicked)
        assertEquals("owner-order-1042", advancedOrderId)
        assertEquals(false, toggledOpen)
    }

    @Test
    fun storeSelectorOpensDropdownAndSwitchesStore() {
        var selectedStoreId: String? = null

        composeRule.setContent {
            CafeTheme {
                OwnerHomeScreen(
                    state = OwnerHomeUiState.Content(
                        storeName = "강남점",
                        isStoreOpen = true,
                        dateLabel = "6월 19일 (금)",
                        stats = OwnerHomeStatsUiModel(
                            totalSales = 0,
                            orderCount = 0,
                            newWaitingCount = 0,
                        ),
                        pendingOrders = emptyList(),
                        isStoreOpenUpdating = false,
                        stores = listOf(
                            OwnerStoreUiModel(id = "store-gangnam", name = "강남점", isSelected = true),
                            OwnerStoreUiModel(id = "store-hongdae", name = "홍대점", isSelected = false),
                        ),
                    ),
                    onToggleStoreOpen = {},
                    onAdvanceStatus = {},
                    onViewAllOrders = {},
                    onRetry = {},
                    onSelectStore = { selectedStoreId = it },
                )
            }
        }

        composeRule.onNodeWithText("강남점 ▾").performClick()
        composeRule.onNodeWithText("홍대점").assertIsDisplayed()
        composeRule.onNodeWithText("홍대점").performClick()

        assertEquals("store-hongdae", selectedStoreId)
    }
}
