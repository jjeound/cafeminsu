package com.cafeminsu.data.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataTest {
    @Test
    fun seedsRequiredMenuData() {
        assertEquals(3, MockData.menuCategories.size)
        assertTrue(MockData.menuItems.size >= 4)
        assertTrue(MockData.menuItems.any { it.isSoldOut })
        assertTrue(MockData.menuItems.any { menu -> menu.options.isNotEmpty() })
    }
}
