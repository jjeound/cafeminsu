package com.cafeminsu.ui.feature.notification.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class NotificationSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitleAndToggleLabels() {
        composeRule.setContent {
            CafeTheme {
                NotificationSettingsScreen(
                    state = NotificationSettingsUiState(),
                    onBackClick = {},
                    onToggle = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("알림 설정").assertIsDisplayed()
        composeRule.onNodeWithText("주문 상태 알림").assertIsDisplayed()
        composeRule.onNodeWithText("혜택·이벤트 알림").assertIsDisplayed()
        composeRule.onNodeWithText("마케팅 정보 수신").assertIsDisplayed()
    }
}
