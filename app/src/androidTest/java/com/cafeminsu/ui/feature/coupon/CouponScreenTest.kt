package com.cafeminsu.ui.feature.coupon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class CouponScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCouponContent() {
        composeRule.setContent {
            CafeTheme {
                CouponScreen(
                    state = CouponUiState.Content(
                        stamp = CouponStampUiModel(
                            storeName = "강남점",
                            currentCount = 7,
                            goalCount = 10,
                        ),
                        coupons = listOf(
                            CouponItemUiModel(
                                id = "coupon-1",
                                title = "무료 음료 1잔 쿠폰",
                                expiresLabel = "유효기간 2026.08.31",
                                available = true,
                                expiringSoon = false,
                            ),
                        ),
                    ),
                    onBackClick = {},
                    onLoginClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("쿠폰").assertIsDisplayed()
        composeRule.onNodeWithText("7 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("보유 쿠폰 (1)").assertIsDisplayed()
        composeRule.onNodeWithText("무료 음료 1잔 쿠폰").assertIsDisplayed()
    }
}
