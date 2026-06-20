package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuDetailRes
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.OptionRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuMapperTest {
    @Test
    fun listItemsDeriveDistinctCategoriesByFirstAppearance() {
        val categories = listOf(
            menuListItem(id = 1, category = "디저트"),
            menuListItem(id = 2, category = "커피"),
            menuListItem(id = 3, category = "커피"),
            menuListItem(id = 4, category = "티"),
        ).toMenuCategories()

        assertEquals(listOf("디저트", "커피", "티"), categories.map { it.id })
        assertEquals(listOf(1, 2, 3), categories.map { it.sortOrder })
    }

    @Test
    fun listItemsMapSoldOutFromAvailability() {
        val result = listOf(
            menuListItem(id = 1, category = "커피", isAvailable = true),
            menuListItem(id = 2, category = "커피", isAvailable = false),
        ).toMenuItems()

        val menus = (result as AppResult.Success).data
        assertEquals(false, menus[0].isSoldOut)
        assertEquals(true, menus[1].isSoldOut)
        assertTrue(menus.all { it.options.isEmpty() })
    }

    @Test
    fun detailResponseGroupsFlatOptionsByOptionGroup() {
        val result = MenuDetailRes(
            id = 101,
            name = "바닐라라떼",
            description = "부드러운 라떼",
            price = 5_500,
            category = "커피",
            imageUrl = "https://cdn.example/latte.png",
            isAvailable = true,
            options = listOf(
                OptionRes(optionId = 1, optionGroup = "온도", optionName = "HOT", optionPrice = 0),
                OptionRes(optionId = 2, optionGroup = "온도", optionName = "ICE", optionPrice = 0),
                OptionRes(optionId = 3, optionGroup = "샷 추가", optionName = "+1샷", optionPrice = 500),
            ),
        ).toMenuItem()

        val menu = (result as AppResult.Success).data
        assertEquals("101", menu.id)
        assertEquals("커피", menu.categoryId)
        assertEquals(listOf("온도", "샷 추가"), menu.options.map { it.name })
        assertEquals(listOf("HOT", "ICE"), menu.options[0].options.map { it.name })
        assertEquals(500, menu.options[1].options.single().extraPrice)
    }

    @Test
    fun missingMenuIdMapsToUnknownError() {
        val result = listOf(menuListItem(id = null, category = "커피")).toMenuItems()

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }

    private fun menuListItem(
        id: Long?,
        category: String?,
        isAvailable: Boolean = true,
    ): MenuListItemRes =
        MenuListItemRes(
            id = id,
            name = "메뉴",
            price = 5_000,
            category = category,
            imageUrl = null,
            isAvailable = isAvailable,
        )
}
