package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuResponseMapperTest {
    @Test
    fun `maps blank response image url to none`() {
        val response = MenuListItemResponse(
            id = 1L,
            name = "아메리카노",
            price = 4_000,
            category = "커피",
            imageUrl = "",
            isAvailable = true,
        )

        val menu = response.asExternalModel()

        assertEquals("", menu.image)
    }

    @Test
    fun `maps blank cached image url to none`() {
        val entity = MenuEntity(
            id = 1L,
            storeId = 1L,
            name = "아메리카노",
            description = "",
            price = 4_000,
            category = "커피",
            imageUrl = "",
            isAvailable = true,
        )

        val menu = entity.asExternalModel()

        assertEquals("", menu.image)
    }
}
