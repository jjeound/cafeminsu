package com.cafeminsu.ui.feature.owner.menu

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

class OwnerMenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOwnerMenuContentAndHandlesActions() {
        var selectedFilter: OwnerMenuFilter? = null
        var toggledMenuId: String? = null
        var addClicked = false

        composeRule.setContent {
            CafeTheme {
                OwnerMenuScreen(
                    state = OwnerMenuUiState.Content(
                        selectedFilter = OwnerMenuFilter.All,
                        filters = OwnerMenuFilter.entries.map { filter ->
                            OwnerMenuFilterUiModel(
                                filter = filter,
                                label = filter.label,
                                selected = filter == OwnerMenuFilter.All,
                            )
                        },
                        menus = listOf(
                            OwnerMenuItemUiModel(
                                id = "americano",
                                name = "아메리카노",
                                price = 4_500,
                                isSoldOut = false,
                                statusLabel = "판매중",
                                isDimmed = false,
                                isActionInProgress = false,
                            ),
                            OwnerMenuItemUiModel(
                                id = "choco-cookie",
                                name = "초코쿠키",
                                price = 3_500,
                                isSoldOut = true,
                                statusLabel = "품절",
                                isDimmed = true,
                                isActionInProgress = false,
                            ),
                        ),
                    ),
                    onFilterSelected = { selectedFilter = it },
                    onSoldOutClick = { toggledMenuId = it },
                    onAddMenuClick = { addClicked = true },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("메뉴 관리").assertIsDisplayed()
        composeRule.onNodeWithText("+ 메뉴 추가").assertIsDisplayed()
        composeRule.onNodeWithText("전체").assertIsDisplayed()
        composeRule.onNodeWithText("커피").assertIsDisplayed()
        composeRule.onNodeWithText("논커피").assertIsDisplayed()
        composeRule.onNodeWithText("디저트").assertIsDisplayed()
        composeRule.onNodeWithText("아메리카노").assertIsDisplayed()
        composeRule.onNodeWithText("₩4,500").assertIsDisplayed()
        composeRule.onNodeWithText("판매중").assertIsDisplayed()
        composeRule.onNodeWithText("초코쿠키").assertIsDisplayed()
        composeRule.onNodeWithText("₩3,500").assertIsDisplayed()
        composeRule.onNodeWithText("품절").assertIsDisplayed()

        composeRule.onNodeWithText("커피").performClick()
        composeRule.onNodeWithContentDescription("아메리카노 판매 상태").performClick()
        composeRule.onNodeWithText("+ 메뉴 추가").performClick()

        assertEquals(OwnerMenuFilter.Coffee, selectedFilter)
        assertEquals("americano", toggledMenuId)
        assertTrue(addClicked)
    }
}
