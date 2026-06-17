package com.cafeminsu.ui.feature.gifticon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class GifticonScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsGifticonPlaceholderTitle() {
        composeRule.setContent {
            CafeTheme {
                GifticonScreen()
            }
        }

        composeRule.onNodeWithText("기프티콘 (M-09)").assertIsDisplayed()
    }
}
