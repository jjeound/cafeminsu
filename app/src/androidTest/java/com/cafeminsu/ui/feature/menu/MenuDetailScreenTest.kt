package com.cafeminsu.ui.feature.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class MenuDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMenuDetailContentAndAddButton() {
        composeRule.setContent {
            CafeTheme {
                MenuDetailScreen(
                    state = MenuDetailUiState.Content(
                        menuItemId = "americano",
                        name = "민수 아메리카노",
                        description = "고소한 블렌드의 깔끔한 기본 커피",
                        basePrice = 5_000,
                        isSoldOut = false,
                        optionGroups = emptyList(),
                        selectedOptionIdsByGroup = emptyMap(),
                        quantity = 1,
                        unitPrice = 5_000,
                        totalPrice = 5_000,
                        canAddToCart = true,
                        addStatus = MenuDetailAddStatus.Idle,
                    ),
                    onOptionToggle = { _, _ -> },
                    onQuantityChange = {},
                    onAddToCart = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("메뉴 상세").assertIsDisplayed()
        composeRule.onNodeWithText("민수 아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("담기 · 5,000원").assertIsDisplayed()
    }
}
