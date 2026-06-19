package com.cafeminsu.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CafeButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAndHandlesClick() {
        var clicks = 0

        composeRule.setContent {
            CafeTheme {
                CafeButton(
                    text = "주문하기",
                    onClick = { clicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("주문하기")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }
}
