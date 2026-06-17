package com.cafeminsu.ui.feature.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuUiStateTest {
    @Test
    fun contentCarriesSelectedCategoryAndMenuItems() {
        val state = MenuUiState.Content(
            categories = listOf(MenuCategoryUiModel(id = "coffee", name = "커피")),
            selectedCategoryId = "coffee",
            menus = listOf(
                MenuItemUiModel(
                    id = "latte",
                    name = "크림 라떼",
                    description = "부드러운 우유와 진한 에스프레소",
                    price = 5_300,
                    isSoldOut = false,
                ),
            ),
        )

        assertEquals("coffee", state.selectedCategoryId)
        assertEquals("latte", state.menus.single().id)
    }

    @Test
    fun emptyCanRetainVisibleCategories() {
        val state = MenuUiState.Empty(
            categories = listOf(MenuCategoryUiModel(id = "dessert", name = "디저트")),
            selectedCategoryId = "dessert",
            message = "선택한 카테고리에 준비된 메뉴가 없어요",
        )

        assertEquals("dessert", state.selectedCategoryId)
        assertTrue(state.categories.isNotEmpty())
    }
}
