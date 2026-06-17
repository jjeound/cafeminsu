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
    fun showsVoicePlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                VoiceScreen()
            }
        }

        composeRule.onNodeWithText("음성 주문 (M-04)").assertIsDisplayed()
    }
}
