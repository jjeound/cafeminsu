package com.cafeminsu.domain.voice

import com.cafeminsu.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsedOrderTest {
    @Test
    fun `keeps parsed items and unmatched utterances`() {
        val selectedOption = SelectedOption(
            groupId = "temperature",
            optionId = "hot",
            name = "Hot",
            extraPrice = 0,
        )
        val item = ParsedOrderItem(
            menuItemId = "latte",
            name = "라떼",
            quantity = 1,
            selectedOptions = listOf(selectedOption),
            isSoldOut = true,
        )
        val parsed = ParsedOrder(
            items = listOf(item),
            unmatched = listOf("피자 하나"),
        )

        assertEquals("latte", parsed.items.first().menuItemId)
        assertEquals(listOf(selectedOption), parsed.items.first().selectedOptions)
        assertTrue(parsed.items.first().isSoldOut)
        assertEquals(listOf("피자 하나"), parsed.unmatched)
    }
}
