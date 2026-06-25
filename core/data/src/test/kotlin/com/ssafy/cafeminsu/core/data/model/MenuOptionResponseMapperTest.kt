package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.network.model.response.menu.MenuOptionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuOptionResponseMapperTest {
    @Test
    fun `maps live menu detail option response to domain option`() {
        val response = MenuOptionResponse(
            id = 108L,
            group = "사이즈",
            name = "Large",
            additionalPrice = 500,
            isDefault = false,
        )

        val option = response.asExternalModel()

        assertEquals(108L, option.id)
        assertEquals("사이즈", option.groupName)
        assertEquals("Large", option.name)
        assertEquals(500, option.additionalPrice)
        assertTrue(!option.isDefault)
    }
}
