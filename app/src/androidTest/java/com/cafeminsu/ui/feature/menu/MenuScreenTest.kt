package com.cafeminsu.ui.feature.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMenuPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                MenuScreen()
            }
        }

        composeRule.onNodeWithText("메뉴 (M-02)").assertIsDisplayed()
    }
}
