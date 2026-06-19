package com.cafeminsu.ui.feature.gifticon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class GifticonScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsGifticonListContent() {
        composeRule.setContent {
            CafeTheme {
                GifticonScreen(
                    state = GifticonListUiState.Content(
                        gifticons = listOf(sampleGifticon()),
                    ),
                    onGifticonClick = {},
                    onStampClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("기프티콘").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노 교환권").assertIsDisplayed()
        composeRule.onNodeWithText("사용 가능").assertIsDisplayed()
    }

    @Test
    fun showsNeedsLoginAction() {
        composeRule.setContent {
            CafeTheme {
                GifticonScreen(
                    state = GifticonListUiState.NeedsLogin(
                        message = "로그인이 필요해요",
                        actionLabel = "다시 로그인하기",
                    ),
                    onGifticonClick = {},
                    onStampClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("로그인이 필요해요").assertIsDisplayed()
        composeRule.onNodeWithText("다시 로그인하기").assertIsDisplayed()
    }

    private fun sampleGifticon(): Gifticon =
        Gifticon(
            id = "gifticon-1",
            title = "아메리카노 교환권",
            barcodeValue = "CAFE-MINSU-GIFT-0001",
            qrValue = "CAFE-MINSU-QR-0001",
            expiresAtMillis = 1_830_297_600_000L,
            status = GifticonStatus.Available,
        )
}
