package com.cafeminsu.ui.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsHomeContent() {
        composeRule.setContent {
            CafeTheme {
                HomeScreen(
                    state = HomeUiState.Content(
                        greeting = "어서 오세요, 카페민수입니다",
                        recommendedMenus = listOf(
                            HomeMenuSummary(
                                id = "menu-1",
                                name = "민수 라떼",
                                description = "고소한 우유와 진한 에스프레소",
                                price = 5200,
                            ),
                        ),
                        stampSummary = HomeStampSummary(
                            currentCount = 4,
                            goalCount = 10,
                        ),
                        ongoingOrder = null,
                    ),
                    onMenuClick = {},
                    onBrowseMenuClick = {},
                    onVoiceOrderClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("어서 오세요, 카페민수입니다").assertIsDisplayed()
        composeRule.onNodeWithText("음성으로 주문하기").assertIsDisplayed()
        composeRule.onNodeWithText("추천 메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("민수 라떼").assertIsDisplayed()
    }
}
