package com.cafeminsu.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CafeTextFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersPlaceholderAndHandlesInput() {
        var value = ""

        composeRule.setContent {
            CafeTheme {
                CafeTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "아이디를 입력하세요",
                )
            }
        }

        composeRule.onNodeWithText("아이디를 입력하세요").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("owner")

        composeRule.runOnIdle {
            assertEquals("owner", value)
        }
    }
}
