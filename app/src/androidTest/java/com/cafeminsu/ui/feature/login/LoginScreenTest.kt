package com.cafeminsu.ui.feature.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCafeBrandAndKakaoLoginButton() {
        composeRule.setContent {
            CafeTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onKakaoLoginClick = {},
                    onOwnerLoginClick = {},
                )
            }
        }

        composeRule.onNodeWithText("카페민수").assertIsDisplayed()
        composeRule.onNodeWithText("카카오 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("점주 로그인").assertIsDisplayed()
    }

    @Test
    fun kakaoLoginButtonHandlesClick() {
        var clicks = 0

        composeRule.setContent {
            CafeTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onKakaoLoginClick = { clicks += 1 },
                    onOwnerLoginClick = {},
                )
            }
        }

        composeRule.onNodeWithText("카카오 로그인").performClick()

        composeRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun ownerLoginLinkHandlesClick() {
        var clicks = 0

        composeRule.setContent {
            CafeTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onKakaoLoginClick = {},
                    onOwnerLoginClick = { clicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("점주 로그인").performClick()

        composeRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }
}
