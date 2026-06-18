package com.cafeminsu.ui.feature.gifticon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class GifticonDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsGifticonDetailValues() {
        composeRule.setContent {
            CafeTheme {
                GifticonDetailScreen(
                    state = GifticonDetailUiState.Content(gifticon = sampleGifticon()),
                    onUse = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("기프티콘 상세").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노 교환권").assertIsDisplayed()
        composeRule.onNodeWithText("CAFE-MINSU-GIFT-0001").assertIsDisplayed()
        composeRule.onNodeWithText("CAFE-MINSU-QR-0001").assertIsDisplayed()
        composeRule.onNodeWithText("사용하기").assertIsDisplayed()
    }

    @Test
    fun disablesUseActionForUsedGifticon() {
        composeRule.setContent {
            CafeTheme {
                GifticonDetailScreen(
                    state = GifticonDetailUiState.Content(
                        gifticon = sampleGifticon(status = GifticonStatus.Used),
                    ),
                    onUse = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("사용하기").assertIsNotEnabled()
    }

    private fun sampleGifticon(
        status: GifticonStatus = GifticonStatus.Available,
    ): Gifticon =
        Gifticon(
            id = "gifticon-1",
            title = "아메리카노 교환권",
            barcodeValue = "CAFE-MINSU-GIFT-0001",
            qrValue = "CAFE-MINSU-QR-0001",
            expiresAtMillis = 1_830_297_600_000L,
            status = status,
        )
}
