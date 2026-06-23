package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuDetailRes
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.MenuOptionRes
import com.cafeminsu.domain.model.MenuItem
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
                MenuOptionRes(id = 1, group = "온도", name = "HOT", additionalPrice = 0, isDefault = true),
                MenuOptionRes(id = 2, group = "온도", name = "ICE", additionalPrice = 0, isDefault = false),
                MenuOptionRes(id = 3, group = "샷 추가", name = "+1샷", additionalPrice = 500, isDefault = false),
            ),
        ).toMenuItem()

        val menu = (result as AppResult.Success).data
        assertEquals("101", menu.id)
        assertEquals("커피", menu.categoryId)
        assertEquals(listOf("온도", "샷 추가"), menu.options.map { it.name })
        assertEquals(listOf("HOT", "ICE"), menu.options[0].options.map { it.name })
        assertEquals(listOf("1", "2"), menu.options[0].options.map { it.id })
        assertEquals(500, menu.options[1].options.single().extraPrice)
    }

    @Test
    fun cachedMenuItemsDeriveDistinctCategoriesByFirstAppearance() {
        // 오프라인 폴백 시 캐시된 메뉴에서 도출한 카테고리가 라이브 목록 결과와 동일해야 한다.
        val categories = listOf(
            menuItem(id = "1", category = "디저트"),
            menuItem(id = "2", category = "커피"),
            menuItem(id = "3", category = "커피"),
            menuItem(id = "4", category = "티"),
            menuItem(id = "5", category = ""),
        ).toMenuCategoriesFromCache()

        assertEquals(listOf("디저트", "커피", "티"), categories.map { it.id })
        assertEquals(listOf("디저트", "커피", "티"), categories.map { it.name })
        assertEquals(listOf(1, 2, 3), categories.map { it.sortOrder })
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

    private fun menuItem(id: String, category: String): MenuItem =
        MenuItem(
            id = id,
            categoryId = category,
            name = "메뉴",
            description = "",
            basePrice = 5_000,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        )
}
