package com.cafeminsu.data.local.menu

import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuCacheMapperTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun roundTripPreservesNestedOptions() {
        val menu = MenuItem(
            id = "101",
            categoryId = "커피",
            name = "바닐라라떼",
            description = "부드러운 라떼",
            basePrice = 5_500,
            imageUrl = "https://cdn.example/latte.png",
            isSoldOut = false,
            options = listOf(
                MenuOptionGroup(
                    id = "온도",
                    name = "온도",
                    required = true,
                    minSelect = 1,
                    maxSelect = 1,
                    options = listOf(
                        MenuOption(id = "1", name = "HOT", extraPrice = 0, isAvailable = true),
                        MenuOption(id = "2", name = "ICE", extraPrice = 0, isAvailable = true),
                    ),
                ),
                MenuOptionGroup(
                    id = "샷 추가",
                    name = "샷 추가",
                    required = false,
                    minSelect = 0,
                    maxSelect = 2,
                    options = listOf(
                        MenuOption(id = "3", name = "+1샷", extraPrice = 500, isAvailable = true),
                    ),
                ),
            ),
            isVisible = true,
        )

        val restored = menu.toMenuEntity(storeId = "11", moshi = moshi).toMenuItem(moshi)

        assertEquals(menu, restored)
    }

    @Test
    fun storeIdAndScalarFieldsAreStoredOnEntity() {
        val menu = MenuItem(
            id = "202",
            categoryId = "디저트",
            name = "바스크 치즈케이크",
            description = "",
            basePrice = 6_800,
            imageUrl = null,
            isSoldOut = true,
            options = emptyList(),
            isVisible = false,
        )

        val entity = menu.toMenuEntity(storeId = "11", moshi = moshi)

        assertEquals("11", entity.storeId)
        assertEquals("202", entity.id)
        assertEquals(true, entity.isSoldOut)
        assertEquals(false, entity.isVisible)
        assertEquals(menu, entity.toMenuItem(moshi))
    }

    @Test
    fun corruptOptionsJsonDecodesToEmptyOptions() {
        // 캐시 JSON 이 손상돼도 화면 오류로 번지지 않도록 빈 옵션으로 흡수한다.
        val entity = MenuEntity(
            id = "303",
            storeId = "11",
            categoryId = "커피",
            name = "아메리카노",
            description = "",
            basePrice = 4_500,
            imageUrl = null,
            isSoldOut = false,
            isVisible = true,
            optionsJson = "not-json",
        )

        assertEquals(emptyList<MenuOptionGroup>(), entity.toMenuItem(moshi).options)
    }
}
