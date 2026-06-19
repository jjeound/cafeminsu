package com.cafeminsu.ui.feature.menu

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsRedesignedMenuListAndVoiceFab() {
        var voiceClicked = false
        var clickedMenuId: String? = null

        composeRule.setContent {
            CafeTheme {
                MenuScreen(
                    state = sampleContentState(),
                    onCategorySelect = {},
                    onMenuClick = { clickedMenuId = it },
                    onVoiceClick = { voiceClicked = true },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("강남점").assertIsDisplayed()
        composeRule.onNodeWithText("오늘의 추천 메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("추천").assertIsDisplayed()
        composeRule.onNodeWithText("커피").assertIsDisplayed()
        composeRule.onNodeWithText("민수 아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("4,500원").assertIsDisplayed()
        composeRule.onNodeWithText("품절").assertIsDisplayed()

        composeRule.onNodeWithText("민수 아메리카노").performClick()
        composeRule.runOnIdle {
            assertEquals("americano", clickedMenuId)
        }

        composeRule.onNodeWithText("아인슈페너").assertHasNoClickAction()

        composeRule.onNodeWithContentDescription("음성 주문").performClick()
        composeRule.runOnIdle {
            assertTrue(voiceClicked)
        }
    }

    private fun sampleContentState(): MenuUiState.Content =
        MenuUiState.Content(
            categories = listOf(
                MenuCategoryUiModel(id = "recommendation", name = "추천"),
                MenuCategoryUiModel(id = "coffee", name = "커피"),
                MenuCategoryUiModel(id = "tea", name = "티"),
            ),
            selectedCategoryId = "recommendation",
            menus = listOf(
                MenuItemUiModel(
                    id = "americano",
                    name = "민수 아메리카노",
                    description = "고소한 블렌드의 깔끔한 기본 커피",
                    price = 4_500,
                    isSoldOut = false,
                ),
                MenuItemUiModel(
                    id = "einspanner",
                    name = "아인슈페너",
                    description = "달콤한 크림을 올린 시그니처 커피",
                    price = 6_000,
                    isSoldOut = true,
                ),
            ),
            storeName = "강남점",
        )
}
