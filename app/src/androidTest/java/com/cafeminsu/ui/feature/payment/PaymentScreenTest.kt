package com.cafeminsu.ui.feature.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class PaymentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsPaymentPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                PaymentScreen()
            }
        }

        composeRule.onNodeWithText("결제 (M-06)").assertIsDisplayed()
    }
}
