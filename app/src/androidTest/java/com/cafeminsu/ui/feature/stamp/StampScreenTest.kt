package com.cafeminsu.ui.feature.stamp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class StampScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsStampPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                StampScreen()
            }
        }

        composeRule.onNodeWithText("스탬프 (M-08)").assertIsDisplayed()
    }
}
