package com.cafeminsu.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startsAtHomeScreen() {
        composeRule.setContent {
            CafeTheme {
                AppNavHost()
            }
        }

        composeRule.onNodeWithText("홈 (M-01)").assertIsDisplayed()
    }

    @Test
    fun clickingMenuTabShowsMenuScreen() {
        composeRule.setContent {
            CafeTheme {
                AppNavHost()
            }
        }

        composeRule.onNodeWithText("메뉴").performClick()

        composeRule.onNodeWithText("메뉴 (M-02)").assertIsDisplayed()
    }
}
