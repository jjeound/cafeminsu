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
    fun showsHomePlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                HomeScreen()
            }
        }

        composeRule.onNodeWithText("홈 (M-01)").assertIsDisplayed()
    }
}
