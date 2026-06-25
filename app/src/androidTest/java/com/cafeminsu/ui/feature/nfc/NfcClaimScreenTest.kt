package com.cafeminsu.ui.feature.nfc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class NfcClaimScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTaggingGuideWhenReady() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(),
                    availability = NfcAvailability.Ready,
                    claimedResult = null,
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("NFC 쿠폰 받기").assertIsDisplayed()
        composeRule.onNodeWithText("폰을 매장 NFC 태그에 대주세요").assertIsDisplayed()
    }

    @Test
    fun showsClaimingProgress() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(claiming = true),
                    availability = NfcAvailability.Ready,
                    claimedResult = null,
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("쿠폰을 발급하고 있어요").assertIsDisplayed()
    }

    @Test
    fun showsInlineError() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(errorMessage = "유효하지 않은 태그예요"),
                    availability = NfcAvailability.Ready,
                    claimedResult = null,
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("유효하지 않은 태그예요").assertIsDisplayed()
    }

    @Test
    fun showsUnsupportedMessage() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(),
                    availability = NfcAvailability.Unsupported,
                    claimedResult = null,
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("이 기기는 NFC를 지원하지 않아요").assertIsDisplayed()
    }

    @Test
    fun showsDisabledMessageWithSettingsAction() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(),
                    availability = NfcAvailability.Disabled,
                    claimedResult = null,
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("NFC가 꺼져 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("NFC 설정 열기").assertIsDisplayed()
    }

    @Test
    fun showsClaimResultDialog() {
        composeRule.setContent {
            CafeTheme {
                NfcClaimScreen(
                    state = NfcClaimUiState(),
                    availability = NfcAvailability.Ready,
                    claimedResult = NfcClaimResultUi(
                        amountLabel = "5,000원",
                        expiresLabel = "2026.12.31 까지",
                        message = "오늘 하루 수고했어요",
                    ),
                    onBackClick = {},
                    onOpenNfcSettings = {},
                    onConfirmClaimed = {},
                )
            }
        }

        composeRule.onNodeWithText("쿠폰이 발급됐어요").assertIsDisplayed()
        composeRule.onNodeWithText("5,000원").assertIsDisplayed()
        composeRule.onNodeWithText("확인").assertIsDisplayed()
    }
}
