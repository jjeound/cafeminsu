package com.cafeminsu.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class CafeCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersContentSlot() {
        composeRule.setContent {
            CafeTheme {
                CafeCard(type = CafeCardType.Product) {
                    Text(text = "아메리카노")
                }
            }
        }

        composeRule.onNodeWithText("아메리카노").assertIsDisplayed()
    }
}
