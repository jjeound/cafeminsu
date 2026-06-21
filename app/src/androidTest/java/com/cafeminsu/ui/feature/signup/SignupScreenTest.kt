package com.cafeminsu.ui.feature.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SignupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersHeaderPlaceholderAndRuleHelper() {
        composeRule.setContent {
            CafeTheme {
                SignupScreen(
                    uiState = SignupUiState(),
                    onNicknameChange = {},
                    onClearClick = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("닉네임을 설정해주세요").assertIsDisplayed()
        composeRule.onNodeWithText("닉네임을 입력해주세요").assertIsDisplayed()
        composeRule.onNodeWithText("한글·영문·숫자 2~10자").assertIsDisplayed()
    }

    @Test
    fun startButtonDisabledWhenNicknameInvalid() {
        composeRule.setContent {
            CafeTheme {
                SignupScreen(
                    uiState = SignupUiState(nickname = "민"),
                    onNicknameChange = {},
                    onClearClick = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("시작하기").assertIsNotEnabled()
    }

    @Test
    fun startButtonEnabledAndClickableWhenValid() {
        var submits = 0
        composeRule.setContent {
            CafeTheme {
                SignupScreen(
                    uiState = SignupUiState(nickname = "민수"),
                    onNicknameChange = {},
                    onClearClick = {},
                    onSubmit = { submits += 1 },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("시작하기").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, submits) }
    }

    @Test
    fun showsErrorMessageWhenPresent() {
        composeRule.setContent {
            CafeTheme {
                SignupScreen(
                    uiState = SignupUiState(
                        nickname = "민수",
                        errorMessage = "이미 사용 중인 닉네임이에요",
                    ),
                    onNicknameChange = {},
                    onClearClick = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("이미 사용 중인 닉네임이에요").assertIsDisplayed()
    }
}
