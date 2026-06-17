package com.cafeminsu.ui.feature.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMenuCategoriesAndProducts() {
        composeRule.setContent {
            CafeTheme {
                MenuScreen(
                    state = sampleContentState(),
                    onCategorySelect = {},
                    onMenuClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("커피").assertIsDisplayed()
        composeRule.onNodeWithText("민수 아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("4500원").assertIsDisplayed()
        composeRule.onNodeWithText("품절").assertIsDisplayed()
    }

    private fun sampleContentState(): MenuUiState.Content =
        MenuUiState.Content(
            categories = listOf(
                MenuCategoryUiModel(id = "coffee", name = "커피"),
                MenuCategoryUiModel(id = "tea", name = "티"),
            ),
            selectedCategoryId = "coffee",
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
        )
}
