package com.cafeminsu.ui.feature.owner.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerLoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersOwnerLoginContent() {
        composeRule.setContent {
            CafeTheme {
                OwnerLoginScreen(
                    uiState = OwnerLoginUiState(),
                    onBackClick = {},
                    onLoginClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("점주 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("매장 관리자 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("카페민수 매장 계정으로 로그인하세요.").assertIsDisplayed()
        composeRule.onNodeWithText("아이디").assertIsDisplayed()
        composeRule.onNodeWithText("비밀번호").assertIsDisplayed()
        composeRule.onNodeWithText("로그인").assertIsDisplayed()
    }

    @Test
    fun prefillsDemoOwnerCredentials() {
        composeRule.setContent {
            CafeTheme {
                OwnerLoginScreen(
                    uiState = OwnerLoginUiState(),
                    onBackClick = {},
                    onLoginClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("owner02").assertIsDisplayed()
    }

    @Test
    fun loginButtonPassesIdAndPasswordWithoutRenderingRawPassword() {
        var submittedLoginId = ""
        var submittedPassword = ""

        composeRule.setContent {
            CafeTheme {
                OwnerLoginScreen(
                    uiState = OwnerLoginUiState(),
                    onBackClick = {},
                    onLoginClick = { loginId, password ->
                        submittedLoginId = loginId
                        submittedPassword = password
                    },
                )
            }
        }

        composeRule.onNodeWithTag("owner-login-id").performTextClearance()
        composeRule.onNodeWithTag("owner-login-id").performTextInput("owner")
        composeRule.onNodeWithTag("owner-login-password").performTextClearance()
        composeRule.onNodeWithTag("owner-login-password").performTextInput("owner-secret")
        composeRule.onNodeWithText("로그인").performClick()

        composeRule.runOnIdle {
            assertEquals("owner", submittedLoginId)
            assertEquals("owner-secret", submittedPassword)
        }
        composeRule.onNodeWithText("owner-secret").assertDoesNotExist()
    }
}
