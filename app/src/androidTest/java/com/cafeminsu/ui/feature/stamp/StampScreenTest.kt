package com.cafeminsu.ui.feature.stamp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.StampEvent
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class StampScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsStampContent() {
        composeRule.setContent {
            CafeTheme {
                StampScreen(
                    state = StampUiState.Content(
                        currentCount = 4,
                        goalCount = 10,
                        history = listOf(sampleStampEvent()),
                    ),
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("스탬프").assertIsDisplayed()
        composeRule.onNodeWithText("4/10").assertIsDisplayed()
        composeRule.onNodeWithText("주문 order-1").assertIsDisplayed()
        composeRule.onNodeWithText("+1개").assertIsDisplayed()
    }

    @Test
    fun showsEmptyStampHistoryAction() {
        composeRule.setContent {
            CafeTheme {
                StampScreen(
                    state = StampUiState.Empty(
                        currentCount = 4,
                        goalCount = 10,
                        message = "아직 적립 내역이 없어요",
                    ),
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("아직 적립 내역이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("메뉴 보러가기").assertIsDisplayed()
    }

    @Test
    fun showsNeedsLoginAction() {
        composeRule.setContent {
            CafeTheme {
                StampScreen(
                    state = StampUiState.NeedsLogin(
                        message = "로그인이 필요해요",
                        actionLabel = "다시 로그인하기",
                    ),
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("로그인이 필요해요").assertIsDisplayed()
        composeRule.onNodeWithText("다시 로그인하기").assertIsDisplayed()
    }

    private fun sampleStampEvent(): StampEvent =
        StampEvent(
            id = "stamp-1",
            orderId = "order-1",
            count = 1,
            createdAtMillis = 1_803_974_400_000L,
        )
}
