package com.cafeminsu.ui.feature.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDetailUiStateTest {
    @Test
    fun contentCarriesSelectionPriceAndAddAvailability() {
        val content = MenuDetailUiState.Content(
            menuItemId = "americano",
            name = "민수 아메리카노",
            description = "고소한 블렌드의 깔끔한 기본 커피",
            basePrice = 5_000,
            isSoldOut = false,
            optionGroups = listOf(
                MenuDetailOptionGroupUiModel(
                    id = "size",
                    name = "사이즈",
                    required = true,
                    minSelect = 1,
                    maxSelect = 1,
                    selectionMode = MenuDetailSelectionMode.Single,
                    selectedOptionIds = setOf("size-large"),
                    options = listOf(
                        MenuDetailOptionUiModel(
                            id = "size-large",
                            name = "라지",
                            extraPrice = 700,
                            isAvailable = true,
                            selected = true,
                        ),
                    ),
                    isSatisfied = true,
                    helperText = "필수 · 1개 선택",
                ),
            ),
            selectedOptionIdsByGroup = mapOf("size" to setOf("size-large")),
            quantity = 2,
            unitPrice = 5_700,
            totalPrice = 11_400,
            canAddToCart = true,
            addStatus = MenuDetailAddStatus.Idle,
        )

        assertEquals(setOf("size-large"), content.selectedOptionIdsByGroup.getValue("size"))
        assertEquals(5_700, content.unitPrice)
        assertEquals(11_400, content.totalPrice)
        assertTrue(content.canAddToCart)
    }

    @Test
    fun errorStateCarriesRetryability() {
        val error = MenuDetailUiState.Error(
            message = "메뉴 정보를 찾지 못했어요",
            retryable = false,
        )

        assertEquals("메뉴 정보를 찾지 못했어요", error.message)
        assertFalse(error.retryable)
    }
}
