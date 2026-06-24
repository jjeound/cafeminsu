package com.cafeminsu.ui.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsHomeContent() {
        var notified = false
        var recommendedMenuId: String? = null
        var reorderedMenuId: String? = null

        composeRule.setContent {
            CafeTheme {
                HomeScreen(
                    state = HomeUiState.Content(
                        greeting = "안녕하세요, 민수님",
                        recommendedMenu = HomeRecommendedMenu(
                            id = "menu-1",
                            name = "민수 시그니처 라떼",
                            description = "고소한 헤이즐넛 시럽 + 따뜻한 우유",
                            price = 5_500,
                            storeName = "민수 강남점",
                        ),
                        recentOrders = listOf(
                            HomeRecentOrderSummary(
                                orderId = "order-1",
                                menuItemId = "americano",
                                menuName = "아메리카노 ICE",
                                optionSummary = "샷 추가 · 톨",
                                orderedAtLabel = "어제",
                                totalPrice = 4_500,
                            ),
                            HomeRecentOrderSummary(
                                orderId = "order-2",
                                menuItemId = "latte",
                                menuName = "헤이즐넛 라떼",
                                optionSummary = "오트밀크 · 그란데",
                                orderedAtLabel = "3일 전",
                                totalPrice = 6_000,
                            ),
                        ),
                    ),
                    onRecommendedOrderClick = { recommendedMenuId = it },
                    onNotificationClick = { notified = true },
                    onRecentOrdersClick = {},
                    onReorderClick = { reorderedMenuId = it },
                    onBrowseMenuClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("안녕하세요, 민수님").assertIsDisplayed()
        composeRule.onNodeWithText("오늘도 잘 부탁드려요").assertIsDisplayed()
        composeRule.onNodeWithText("오늘의 추천 메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("민수 강남점").assertIsDisplayed()
        composeRule.onNodeWithText("민수 시그니처 라떼").assertIsDisplayed()
        composeRule.onNodeWithText("5,500원").assertIsDisplayed()
        composeRule.onNodeWithText("다시 주문하기").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노 ICE").assertIsDisplayed()

        composeRule.onNodeWithText("지금 주문하기 ›").performClick()
        composeRule.onNodeWithContentDescription("알림").performClick()
        composeRule.onNodeWithText("4,500원 · 재주문").performClick()

        assertEquals("menu-1", recommendedMenuId)
        assertEquals(true, notified)
        assertEquals("americano", reorderedMenuId)
    }
}
