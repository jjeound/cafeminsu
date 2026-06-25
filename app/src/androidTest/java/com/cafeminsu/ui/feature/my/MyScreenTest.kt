package com.cafeminsu.ui.feature.my

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsProfileStatsQuickMenusAndSettings() {
        composeRule.setContent {
            CafeTheme {
                MyScreen(
                    state = sampleContent(),
                    onHistoryClick = {},
                    onGiftClick = {},
                    onCouponClick = {},
                    onNotificationSettingsClick = {},
                    onLoginClick = {},
                    onLogoutClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("MY").assertIsDisplayed()
        composeRule.onNodeWithText("진지원 님").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithText("7/10").assertExists()
        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText("주문내역").assertExists()
        composeRule.onNodeWithText("선물하기").assertExists()
        composeRule.onNodeWithText("쿠폰").assertExists()
        composeRule.onNodeWithText("알림설정").assertExists()
        composeRule.onNodeWithText("이용 약관").assertExists()
        composeRule.onNodeWithText("자주 묻는 질문").assertExists()
        composeRule.onNodeWithText("고객센터").assertExists()
        composeRule.onNodeWithText("1588-1234").assertExists()
        composeRule.onNodeWithText("버전 정보").assertExists()
        composeRule.onNodeWithText("v1.0.0").assertExists()
        composeRule.onNodeWithText("로그아웃").assertExists()
    }

    @Test
    fun logoutRowShowsConfirmDialog() {
        var logoutConfirmed = false

        composeRule.setContent {
            CafeTheme {
                MyScreen(
                    state = sampleContent(),
                    onHistoryClick = {},
                    onGiftClick = {},
                    onCouponClick = {},
                    onNotificationSettingsClick = {},
                    onLoginClick = {},
                    onLogoutClick = { logoutConfirmed = true },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("로그아웃").performClick()
        composeRule.onNodeWithText("로그아웃 하시겠어요?").assertIsDisplayed()
        composeRule.onNodeWithText("로그인 정보를 잊지 않도록\n계정 정보를 확인해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("취소").assertIsDisplayed()
        composeRule.onAllNodesWithText("로그아웃")[1].performClick()
        assertTrue(logoutConfirmed)
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
                    onHistoryClick = {},
                    onGiftClick = {},
                    onCouponClick = {},
                    onNotificationSettingsClick = {},
                    onLoginClick = {},
                    onLogoutClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("로그인이 필요해요").assertIsDisplayed()
        composeRule.onNodeWithText("다시 로그인하기").assertIsDisplayed()
    }

    private fun sampleContent(): MyUiState.Content =
        MyUiState.Content(
            profile = MyProfileUiModel(
                displayName = "진지원",
                initial = "진",
                tierLabel = "",
            ),
            stats = MyStatsUiModel(
                orderCount = 12,
                stampCount = 7,
                stampGoalCount = 10,
                couponCount = 3,
            ),
            quickMenus = listOf(
                MyQuickMenuUiModel(id = "history", label = "주문내역"),
                MyQuickMenuUiModel(id = "gift", label = "선물하기"),
                MyQuickMenuUiModel(id = "coupon", label = "쿠폰"),
                MyQuickMenuUiModel(id = "notification_settings", label = "알림설정"),
            ),
            settings = listOf(
                MySettingItemUiModel(id = "terms", label = "이용 약관"),
                MySettingItemUiModel(id = "faq", label = "자주 묻는 질문"),
                MySettingItemUiModel(id = "support", label = "고객센터", trailingText = "1588-1234"),
                MySettingItemUiModel(id = "version", label = "버전 정보", trailingText = "v1.0.0"),
                MySettingItemUiModel(id = "logout", label = "로그아웃", isDestructive = true),
            ),
        )
}
