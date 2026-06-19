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
                        optionGroups = sampleOptionGroups(),
                        selectedOptionIdsByGroup = mapOf(
                            "temperature" to setOf("temperature-ice"),
                            "size" to setOf("size-regular"),
                            "shot" to setOf("shot-none"),
                        ),
                        quantity = 1,
                        unitPrice = 5_000,
                        totalPrice = 5_000,
                        canAddToCart = true,
                        addStatus = MenuDetailAddStatus.Idle,
                    ),
                    onBackClick = {},
                    onOptionToggle = { _, _ -> },
                    onQuantityChange = {},
                    onAddToCart = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("메뉴 상세").assertIsDisplayed()
        composeRule.onNodeWithText("민수 아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("온도").assertIsDisplayed()
        composeRule.onNodeWithText("HOT").assertIsDisplayed()
        composeRule.onNodeWithText("ICE").assertIsDisplayed()
        composeRule.onNodeWithText("사이즈").assertIsDisplayed()
        composeRule.onNodeWithText("Regular").assertIsDisplayed()
        composeRule.onNodeWithText("Large (+500)").assertIsDisplayed()
        composeRule.onNodeWithText("샷 추가").assertIsDisplayed()
        composeRule.onNodeWithText("없음").assertIsDisplayed()
        composeRule.onNodeWithText("+1샷 (+500)").assertIsDisplayed()
        composeRule.onNodeWithText("+2샷 (+1,000)").assertIsDisplayed()
        composeRule.onNodeWithText("수량").assertIsDisplayed()
        composeRule.onNodeWithText("장바구니 담기 · 5,000원").assertIsDisplayed()
    }

    private fun sampleOptionGroups(): List<MenuDetailOptionGroupUiModel> =
        listOf(
            MenuDetailOptionGroupUiModel(
                id = "temperature",
                name = "온도",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                selectionMode = MenuDetailSelectionMode.Single,
                selectedOptionIds = setOf("temperature-ice"),
                options = listOf(
                    MenuDetailOptionUiModel(
                        id = "temperature-hot",
                        name = "HOT",
                        extraPrice = 0,
                        isAvailable = true,
                        selected = false,
                    ),
                    MenuDetailOptionUiModel(
                        id = "temperature-ice",
                        name = "ICE",
                        extraPrice = 0,
                        isAvailable = true,
                        selected = true,
                    ),
                ),
                isSatisfied = true,
                helperText = "필수 · 1개 선택",
            ),
            MenuDetailOptionGroupUiModel(
                id = "size",
                name = "사이즈",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                selectionMode = MenuDetailSelectionMode.Single,
                selectedOptionIds = setOf("size-regular"),
                options = listOf(
                    MenuDetailOptionUiModel(
                        id = "size-regular",
                        name = "Regular",
                        extraPrice = 0,
                        isAvailable = true,
                        selected = true,
                    ),
                    MenuDetailOptionUiModel(
                        id = "size-large",
                        name = "Large",
                        extraPrice = 500,
                        isAvailable = true,
                        selected = false,
                    ),
                ),
                isSatisfied = true,
                helperText = "필수 · 1개 선택",
            ),
            MenuDetailOptionGroupUiModel(
                id = "shot",
                name = "샷 추가",
                required = false,
                minSelect = 0,
                maxSelect = 1,
                selectionMode = MenuDetailSelectionMode.Single,
                selectedOptionIds = setOf("shot-none"),
                options = listOf(
                    MenuDetailOptionUiModel(
                        id = "shot-none",
                        name = "없음",
                        extraPrice = 0,
                        isAvailable = true,
                        selected = true,
                    ),
                    MenuDetailOptionUiModel(
                        id = "shot-one",
                        name = "+1샷",
                        extraPrice = 500,
                        isAvailable = true,
                        selected = false,
                    ),
                    MenuDetailOptionUiModel(
                        id = "shot-two",
                        name = "+2샷",
                        extraPrice = 1_000,
                        isAvailable = true,
                        selected = false,
                    ),
                ),
                isSatisfied = true,
                helperText = "선택 · 1개 선택",
            ),
        )
}
