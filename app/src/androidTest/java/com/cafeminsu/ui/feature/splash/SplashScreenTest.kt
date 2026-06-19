package com.cafeminsu.ui.feature.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class SplashScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCafeMinsuBrandAndTagline() {
        composeRule.setContent {
            CafeTheme {
                SplashScreen()
            }
        }

        composeRule.onNodeWithText("카페민수").assertIsDisplayed()
        composeRule.onNodeWithText("Warm cream coffee").assertIsDisplayed()
    }
}
