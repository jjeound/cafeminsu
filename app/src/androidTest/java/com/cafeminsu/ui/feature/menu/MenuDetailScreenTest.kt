package com.cafeminsu.ui.feature.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MenuDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMenuDetailPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                MenuDetailScreen()
            }
        }

        composeRule.onNodeWithText("메뉴 상세 (M-03)").assertIsDisplayed()
    }
}
