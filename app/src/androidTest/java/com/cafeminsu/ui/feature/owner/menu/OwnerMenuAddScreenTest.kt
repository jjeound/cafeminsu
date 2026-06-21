package com.cafeminsu.ui.feature.owner.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OwnerMenuAddScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersFormAndDisablesSaveWhenInvalid() {
        var submitted = false

        composeRule.setContent {
            CafeTheme {
                OwnerMenuAddScreen(
                    uiState = OwnerMenuAddUiState(),
                    onImagePicked = {},
                    onCategorySelected = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onDescriptionChange = {},
                    onSaleToggle = {},
                    onSubmit = { submitted = true },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("메뉴 추가").assertIsDisplayed()
        composeRule.onNodeWithText("대표 사진 추가").assertIsDisplayed()
        composeRule.onNodeWithText("커피").assertIsDisplayed()
        composeRule.onNodeWithText("저장하기").assertIsDisplayed()

        composeRule.onNodeWithText("저장하기").performClick()
        assertFalse(submitted)
    }

    @Test
    fun invokesSubmitWhenFormIsValid() {
        var submitted = false

        composeRule.setContent {
            CafeTheme {
                OwnerMenuAddScreen(
                    uiState = OwnerMenuAddUiState(name = "아메리카노", priceInput = "4500"),
                    onImagePicked = {},
                    onCategorySelected = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onDescriptionChange = {},
                    onSaleToggle = {},
                    onSubmit = { submitted = true },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("저장하기").performClick()
        assertTrue(submitted)
    }

    @Test
    fun backIconInvokesOnBack() {
        var backClicked = false

        composeRule.setContent {
            CafeTheme {
                OwnerMenuAddScreen(
                    uiState = OwnerMenuAddUiState(),
                    onImagePicked = {},
                    onCategorySelected = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onDescriptionChange = {},
                    onSaleToggle = {},
                    onSubmit = {},
                    onBack = { backClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("뒤로").performClick()
        assertTrue(backClicked)
    }
}
