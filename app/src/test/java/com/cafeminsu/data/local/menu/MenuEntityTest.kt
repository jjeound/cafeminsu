package com.cafeminsu.data.local.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuEntityTest {
    @Test
    fun retainsValuesAndSupportsCopy() {
        val entity = MenuEntity(
            id = "101",
            storeId = "11",
            categoryId = "커피",
            name = "바닐라라떼",
            description = "부드러운 라떼",
            basePrice = 5_500,
            imageUrl = "https://cdn.example/latte.png",
            isSoldOut = false,
            isVisible = true,
            optionsJson = "[]",
        )

        assertEquals("101", entity.id)
        assertEquals("11", entity.storeId)
        assertEquals("[]", entity.optionsJson)
        assertEquals(true, entity.copy(isSoldOut = true).isSoldOut)
        assertEquals(entity, entity.copy())
    }
}
