package com.cafeminsu.ui.feature.gift

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class GiftScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsGiftFormContent() {
        composeRule.setContent {
            CafeTheme {
                GiftScreen(
                    state = GiftUiState.Content(
                        selectedAmountOption = GiftAmountOption.TenThousand,
                        selectedChannel = GiftChannel.KakaoTalk,
                        recipient = "",
                        message = "오늘 하루 수고 많았어",
                    ),
                    onBackClick = {},
                    onLoginClick = {},
                    onRetry = {},
                    onAmountSelected = {},
                    onChannelSelected = {},
                    onRecipientChanged = {},
                    onMessageChanged = {},
                    onSendClick = {},
                )
            }
        }

        composeRule.onNodeWithText("선물하기").assertIsDisplayed()
        composeRule.onNodeWithText("₩ 10,000").assertIsDisplayed()
        composeRule.onNodeWithText("구매하고 선물 보내기 · 10,000원").assertIsDisplayed()
    }

    @Test
    fun showsBothChannelCardsWithTitleAndSubtitle() {
        composeRule.setContent {
            CafeTheme {
                GiftScreen(
                    state = GiftUiState.Content(
                        selectedAmountOption = GiftAmountOption.TenThousand,
                        selectedChannel = GiftChannel.KakaoTalk,
                        recipient = "",
                        message = "",
                    ),
                    onBackClick = {},
                    onLoginClick = {},
                    onRetry = {},
                    onAmountSelected = {},
                    onChannelSelected = {},
                    onRecipientChanged = {},
                    onMessageChanged = {},
                    onSendClick = {},
                )
            }
        }

        composeRule.onNodeWithText("카카오톡").assertIsDisplayed()
        composeRule.onNodeWithText("친구 선택").assertIsDisplayed()
        composeRule.onNodeWithText("문자 (SMS)").assertIsDisplayed()
        composeRule.onNodeWithText("연락처 입력").assertIsDisplayed()
    }
}
