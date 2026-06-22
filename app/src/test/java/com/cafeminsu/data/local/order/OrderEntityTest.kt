package com.cafeminsu.data.local.order

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderEntityTest {
    @Test
    fun retainsValuesAndSupportsCopy() {
        val entity = OrderEntity(
            id = "77",
            orderNumber = "A-2543",
            totalAmount = 10_000,
            status = "Completed",
            createdAtMillis = 1_700_000_000_000L,
            itemsJson = "[]",
        )

        assertEquals("77", entity.id)
        assertEquals("A-2543", entity.orderNumber)
        assertEquals("Completed", entity.status)
        assertEquals("[]", entity.itemsJson)
        assertEquals(13_000, entity.copy(totalAmount = 13_000).totalAmount)
        assertEquals(entity, entity.copy())
    }
}
