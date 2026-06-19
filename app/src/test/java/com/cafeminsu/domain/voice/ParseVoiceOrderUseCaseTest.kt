package com.cafeminsu.domain.voice

import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseVoiceOrderUseCaseTest {
    private val parser = ParseVoiceOrderUseCase()

    @Test
    fun `parses korean quantity counter`() {
        val parsed = parser("아메리카노 두 잔", seedMenu)

        assertEquals(1, parsed.items.size)
        assertEquals("americano", parsed.items.first().menuItemId)
        assertEquals("아메리카노", parsed.items.first().name)
        assertEquals(2, parsed.items.first().quantity)
        assertTrue(parsed.items.first().selectedOptions.isEmpty())
        assertTrue(parsed.unmatched.isEmpty())
    }

    @Test
    fun `parses hot temperature option when menu supports it`() {
        val parsed = parser("라떼 한 잔 따뜻하게", seedMenu)

        assertEquals(1, parsed.items.size)
        val item = parsed.items.first()
        assertEquals("latte", item.menuItemId)
        assertEquals(1, item.quantity)
        assertEquals(1, item.selectedOptions.size)
        assertEquals("temperature", item.selectedOptions.first().groupId)
        assertEquals("hot", item.selectedOptions.first().optionId)
        assertEquals("Hot", item.selectedOptions.first().name)
    }

    @Test
    fun `parses multiple menu items separated by connector`() {
        val parsed = parser("아메리카노랑 라떼", seedMenu)

        assertEquals(2, parsed.items.size)
        assertEquals("americano", parsed.items[0].menuItemId)
        assertEquals(1, parsed.items[0].quantity)
        assertEquals("latte", parsed.items[1].menuItemId)
        assertEquals(1, parsed.items[1].quantity)
        assertTrue(parsed.unmatched.isEmpty())
    }

    @Test
    fun `puts unknown utterance into unmatched`() {
        val parsed = parser("피자 하나", seedMenu)

        assertTrue(parsed.items.isEmpty())
        assertEquals(listOf("피자 하나"), parsed.unmatched)
    }

    @Test
    fun `defaults quantity to one when omitted`() {
        val parsed = parser("아메리카노", seedMenu)

        assertEquals(1, parsed.items.size)
        assertEquals(1, parsed.items.first().quantity)
    }

    @Test
    fun `returns empty parsed order for blank transcript`() {
        val parsed = parser("   ", seedMenu)

        assertTrue(parsed.items.isEmpty())
        assertTrue(parsed.unmatched.isEmpty())
    }

    private companion object {
        private val temperatureGroup = MenuOptionGroup(
            id = "temperature",
            name = "온도",
            required = true,
            minSelect = 1,
            maxSelect = 1,
            options = listOf(
                MenuOption(
                    id = "hot",
                    name = "Hot",
                    extraPrice = 0,
                    isAvailable = true,
                ),
                MenuOption(
                    id = "iced",
                    name = "Iced",
                    extraPrice = 0,
                    isAvailable = true,
                ),
            ),
        )

        private val seedMenu = listOf(
            MenuItem(
                id = "americano",
                categoryId = "coffee",
                name = "아메리카노",
                description = "고소한 기본 커피",
                basePrice = 4_000,
                imageUrl = null,
                isSoldOut = false,
                options = listOf(temperatureGroup),
            ),
            MenuItem(
                id = "latte",
                categoryId = "coffee",
                name = "라떼",
                description = "우유가 들어간 커피",
                basePrice = 4_500,
                imageUrl = null,
                isSoldOut = false,
                options = listOf(temperatureGroup),
            ),
        )
    }
}
