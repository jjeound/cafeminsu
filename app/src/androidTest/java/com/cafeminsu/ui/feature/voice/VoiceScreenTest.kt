package com.cafeminsu.ui.feature.voice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class VoiceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsIdleVoiceOrderScreen() {
        composeRule.setContent {
            CafeTheme {
                VoiceScreen(
                    state = VoiceUiState.Idle,
                    onRequestPermission = {},
                    onConfirm = {},
                    onRetry = {},
                    onNavigateToMenu = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("음성으로 주문").assertIsDisplayed()
        composeRule.onNodeWithText("원하시는 메뉴를\n말씀해주세요").assertIsDisplayed()
        composeRule.onNodeWithText("인식된 음성").assertIsDisplayed()
        composeRule.onNodeWithText("AI 인식 결과").assertIsDisplayed()
        composeRule.onNodeWithText("다시 말하기").assertIsDisplayed()
        composeRule.onNodeWithText("이대로 주문").assertIsDisplayed()
    }

    @Test
    fun showsParsedVoiceOrderResult() {
        composeRule.setContent {
            CafeTheme {
                VoiceScreen(
                    state = VoiceUiState.Parsed(
                        transcript = "아이스 바닐라라떼 한 잔",
                        items = listOf(
                            ParsedOrderItem(
                                menuItemId = "vanilla-latte",
                                name = "바닐라라떼",
                                quantity = 1,
                                selectedOptions = listOf(
                                    SelectedOption(
                                        groupId = "temperature",
                                        optionId = "iced",
                                        name = "Iced",
                                        extraPrice = 0,
                                    ),
                                    SelectedOption(
                                        groupId = "size",
                                        optionId = "regular",
                                        name = "Regular",
                                        extraPrice = 0,
                                    ),
                                ),
                            ),
                        ),
                        unmatched = emptyList(),
                        estimatedTotalAmount = 5_500,
                        confidencePercent = 97,
                    ),
                    onRequestPermission = {},
                    onConfirm = {},
                    onRetry = {},
                    onNavigateToMenu = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("신뢰도 97%").assertIsDisplayed()
        composeRule.onNodeWithText("바닐라라떼 · ICE · Regular").assertIsDisplayed()
        composeRule.onNodeWithText("예상 금액").assertIsDisplayed()
        composeRule.onNodeWithText("5,500원").assertIsDisplayed()
    }
}
