package com.cafeminsu.ui.feature.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class CartScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCartPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                CartScreen()
            }
        }

        composeRule.onNodeWithText("장바구니 (M-05)").assertIsDisplayed()
    }
}
