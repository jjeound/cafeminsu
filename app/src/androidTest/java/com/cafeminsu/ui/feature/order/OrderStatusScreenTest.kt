package com.cafeminsu.ui.feature.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class OrderStatusScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOrderStatusPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                OrderStatusScreen()
            }
        }

        composeRule.onNodeWithText("주문 상태 (M-07)").assertIsDisplayed()
    }
}
