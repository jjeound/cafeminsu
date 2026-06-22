package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuApiTest {
    @Test
    fun menuListDtoKeepsOpenApiFields() {
        val item = MenuListItemRes(
            id = 101,
            name = "바닐라라떼",
            price = 5_500,
            category = "커피",
            imageUrl = "https://cdn.example/latte.png",
            isAvailable = true,
        )

        assertEquals(101L, item.id)
        assertEquals("바닐라라떼", item.name)
        assertEquals(5_500, item.price)
        assertEquals("커피", item.category)
    }

    @Test
    fun menuDetailDtoKeepsLiveServerOptionFields() {
        val detail = MenuDetailRes(
            id = 101,
            name = "바닐라라떼",
            description = "부드러운 라떼",
            price = 5_500,
            category = "커피",
            imageUrl = null,
            isAvailable = true,
            options = listOf(
                MenuOptionRes(
                    id = 1,
                    group = "온도",
                    name = "ICE",
                    additionalPrice = 0,
                    isDefault = false,
                ),
            ),
        )

        assertEquals("온도", detail.options?.single()?.group)
        assertEquals("ICE", detail.options?.single()?.name)
    }
}
