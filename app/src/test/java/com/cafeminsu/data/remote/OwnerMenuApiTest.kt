package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuApiTest {
    @Test
    fun menuCreateReqKeepsOpenApiFields() {
        val request = MenuCreateReq(
            name = "콜드브루",
            description = "깊고 진한 콜드브루",
            price = 5_500,
            category = "coffee",
            imageUrl = null,
            isAvailable = true,
        )

        assertEquals("콜드브루", request.name)
        assertEquals("깊고 진한 콜드브루", request.description)
        assertEquals(5_500, request.price)
        assertEquals("coffee", request.category)
        assertEquals(null, request.imageUrl)
        assertTrue(request.isAvailable)
    }

    @Test
    fun menuCreateResAndAvailabilityReqKeepOpenApiFields() {
        val created = MenuCreateRes(menuId = 555)
        val availability = MenuAvailabilityReq(isAvailable = false)

        assertEquals(555L, created.menuId)
        assertFalse(availability.isAvailable)
    }
}
