package com.cafeminsu.ui.feature.my

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMyPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                MyScreen()
            }
        }

        composeRule.onNodeWithText("마이페이지 (M-10)").assertIsDisplayed()
    }
}
