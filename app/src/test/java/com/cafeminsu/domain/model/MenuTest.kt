package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuTest {
    @Test
    fun exposesMenuDomainModels() {
        val option = MenuOption(
            id = "shot",
            name = "샷 추가",
            extraPrice = 500,
            isAvailable = true,
        )
        val optionGroup = MenuOptionGroup(
            id = "espresso",
            name = "에스프레소",
            required = false,
            minSelect = 0,
            maxSelect = 1,
            options = listOf(option),
        )
        val category = MenuCategory(
            id = "coffee",
            name = "커피",
            sortOrder = 1,
        )
        val item = MenuItem(
            id = "americano",
            categoryId = category.id,
            name = "아메리카노",
            description = "고소한 원두",
            basePrice = 4500,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(optionGroup),
        )

        assertEquals("coffee", category.id)
        assertEquals(4500, item.basePrice)
        assertNull(item.imageUrl)
        assertFalse(item.isSoldOut)
        assertTrue(item.options.single().options.single().isAvailable)
    }
}
