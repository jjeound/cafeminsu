package com.cafeminsu.ui.feature.voice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class VoiceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsIdleVoiceOrderScreen() {
        composeRule.setContent {
            CafeTheme {
                VoiceScreen(
                    state = VoiceUiState.Idle,
                    onRequestPermission = {},
                    onConfirm = {},
                    onRetry = {},
                    onNavigateToMenu = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("음성 주문").assertIsDisplayed()
        composeRule.onNodeWithText("음성 주문 시작").assertIsDisplayed()
        composeRule.onNodeWithText("들은 내용").assertIsDisplayed()
    }
}
