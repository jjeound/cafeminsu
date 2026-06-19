package com.cafeminsu.ui.feature.my

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsProfileOrderHistorySettingsAndMaskedPhone() {
        composeRule.setContent {
            CafeTheme {
                MyScreen(
                    state = MyUiState.Content(
                        profile = MyProfileUiModel(
                            displayName = "민수",
                            phoneLast4 = "1234",
                        ),
                        recentOrders = listOf(sampleOrderSummary()),
                        settings = listOf(MySettingItemUiModel(id = "logout", label = "로그아웃")),
                        appMeta = "앱 버전 1.0",
                    ),
                    onOrderClick = {},
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onLogoutClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("마이페이지").assertIsDisplayed()
        composeRule.onNodeWithText("민수").assertIsDisplayed()
        composeRule.onNodeWithText("010-****-1234").assertIsDisplayed()
        composeRule.onNodeWithText("주문번호 M001").assertExists()
        composeRule.onNodeWithText("결제 완료").assertExists()
        composeRule.onNodeWithText("로그아웃").assertExists()
        composeRule.onNodeWithText("앱 버전 1.0").assertExists()
    }

    @Test
    fun showsEmptyOrderHistoryAction() {
        composeRule.setContent {
            CafeTheme {
                MyScreen(
                    state = MyUiState.Empty(
                        profile = MyProfileUiModel(
                            displayName = "민수",
                            phoneLast4 = "1234",
                        ),
                        message = "주문 내역이 없어요",
                        actionLabel = "메뉴 보러가기",
                        settings = listOf(MySettingItemUiModel(id = "logout", label = "로그아웃")),
                        appMeta = "앱 버전 1.0",
                    ),
                    onOrderClick = {},
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onLogoutClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("주문 내역이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("메뉴 보러가기").assertIsDisplayed()
    }

    @Test
    fun showsNeedsLoginAction() {
        composeRule.setContent {
            CafeTheme {
                MyScreen(
                    state = MyUiState.NeedsLogin(
                        message = "로그인이 필요해요",
                        actionLabel = "다시 로그인하기",
                    ),
                    onOrderClick = {},
                    onBrowseMenuClick = {},
                    onLoginClick = {},
                    onLogoutClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("로그인이 필요해요").assertIsDisplayed()
        composeRule.onNodeWithText("다시 로그인하기").assertIsDisplayed()
    }

    private fun sampleOrderSummary(): MyOrderSummaryUiModel =
        MyOrderSummaryUiModel(
            orderId = "order-1",
            orderNumber = "M001",
            createdAtMillis = 1_803_974_400_000L,
            totalAmount = 5_500,
            statusLabel = "결제 완료",
        )
}
