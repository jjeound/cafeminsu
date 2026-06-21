package com.cafeminsu.ui.feature.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.NotificationType
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotiScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersGroupedNotificationContentAndBackAction() {
        var backClicks = 0

        composeRule.setContent {
            CafeTheme {
                NotiScreen(
                    state = NotiUiState.Content(
                        groups = listOf(
                            NotiGroupUiModel(
                                label = "오늘",
                                items = listOf(
                                    NotiItemUiModel(
                                        id = "noti-1",
                                        type = NotificationType.OrderReady,
                                        title = "주문이 준비됐어요",
                                        body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
                                        timeLabel = "방금",
                                        unread = true,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onBackClick = { backClicks += 1 },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("알림").assertIsDisplayed()
        composeRule.onNodeWithText("오늘").assertIsDisplayed()
        composeRule.onNodeWithText("주문이 준비됐어요").assertIsDisplayed()
        composeRule.onNodeWithText("방금").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒤로").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backClicks)
        }
    }
}
