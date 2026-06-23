package com.cafeminsu.data.local.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEntityTest {
    @Test
    fun retainsValuesAndSupportsCopy() {
        val entity = NotificationEntity(
            id = "71",
            type = "OrderReady",
            title = "주문이 준비됐어요",
            body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
            createdAtMillis = 1_700_000_000_000L,
            read = false,
        )

        assertEquals("71", entity.id)
        assertEquals("OrderReady", entity.type)
        assertEquals(false, entity.read)
        assertEquals(true, entity.copy(read = true).read)
        assertEquals(entity, entity.copy())
    }
}
