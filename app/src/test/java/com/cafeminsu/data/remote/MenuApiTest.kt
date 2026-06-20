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
    fun menuDetailDtoKeepsFlatOptionFields() {
        val detail = MenuDetailRes(
            id = 101,
            name = "바닐라라떼",
            description = "부드러운 라떼",
            price = 5_500,
            category = "커피",
            imageUrl = null,
            isAvailable = true,
            options = listOf(
                OptionRes(
                    optionId = 1,
                    optionGroup = "온도",
                    optionName = "ICE",
                    optionPrice = 0,
                ),
            ),
        )

        assertEquals("온도", detail.options?.single()?.optionGroup)
        assertEquals("ICE", detail.options?.single()?.optionName)
    }
}
