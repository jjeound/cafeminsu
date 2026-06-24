package com.cafeminsu.core.data.model

import com.cafeminsu.core.network.model.response.menu.MenuOptionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuOptionResponseMapperTest {
    @Test
    fun `maps new menu detail option response fields to domain option`() {
        val response = MenuOptionResponse(
            optionId = 12L,
            optionGroup = "온도",
            optionName = "ICE",
            optionPrice = 500,
            isDefault = true,
        )

        val option = response.asExternalModel()

        assertEquals(12L, option.id)
        assertEquals("온도", option.groupName)
        assertEquals("ICE", option.name)
        assertEquals(500, option.additionalPrice)
        assertTrue(option.isDefault)
    }
}
