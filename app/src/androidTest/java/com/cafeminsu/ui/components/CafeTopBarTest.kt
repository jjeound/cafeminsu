package com.cafeminsu.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CafeTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitleAndHandlesSlotClicks() {
        var backClicks = 0
        var actionClicks = 0

        composeRule.setContent {
            CafeTheme {
                CafeTopBar(
                    title = "메뉴",
                    navigationIcon = { Text(text = "뒤") },
                    onNavigationClick = { backClicks += 1 },
                    actionIcon = { Text(text = "담") },
                    onActionClick = { actionClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("뒤").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("담").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, backClicks)
            assertEquals(1, actionClicks)
        }
    }
}
