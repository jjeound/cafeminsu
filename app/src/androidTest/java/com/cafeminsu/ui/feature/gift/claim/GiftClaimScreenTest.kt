package com.cafeminsu.ui.feature.gift.claim

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class GiftClaimScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsClaimFormWithPrefilledCode() {
        composeRule.setContent {
            CafeTheme {
                GiftClaimScreen(
                    state = GiftClaimUiState(code = "GFT-1234-5678"),
                    onBackClick = {},
                    onCodeChanged = {},
                    onClaimClick = {},
                )
            }
        }

        composeRule.onNodeWithText("선물 등록").assertIsDisplayed()
        composeRule.onNodeWithText("GFT-1234-5678").assertIsDisplayed()
        composeRule.onNodeWithText("등록하기").assertIsDisplayed()
    }

    @Test
    fun submitDisabledWhenCodeBlank() {
        composeRule.setContent {
            CafeTheme {
                GiftClaimScreen(
                    state = GiftClaimUiState(code = ""),
                    onBackClick = {},
                    onCodeChanged = {},
                    onClaimClick = {},
                )
            }
        }

        composeRule.onNodeWithText("등록하기").assertIsNotEnabled()
    }

    @Test
    fun showsErrorMessage() {
        composeRule.setContent {
            CafeTheme {
                GiftClaimScreen(
                    state = GiftClaimUiState(
                        code = "GFT-1234-5678",
                        errorMessage = "등록할 수 없는 코드예요. 코드를 다시 확인해 주세요",
                    ),
                    onBackClick = {},
                    onCodeChanged = {},
                    onClaimClick = {},
                )
            }
        }

        composeRule.onNodeWithText("등록할 수 없는 코드예요. 코드를 다시 확인해 주세요").assertIsDisplayed()
    }
}
