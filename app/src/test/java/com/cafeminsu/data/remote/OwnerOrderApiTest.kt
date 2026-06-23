package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerOrderApiTest {
    @Test
    fun storeOrderDtosKeepOpenApiFields() {
        val item = StoreOrderItemRes(
            orderId = 1042,
            orderNumber = "1042",
            status = "PENDING",
            totalAmount = 9_300,
            items = listOf(
                MenuSummary(menuId = 101, menuName = "아메리카노", quantity = 2),
            ),
            createdAt = "2026-06-20T01:15:30Z",
        )

        assertEquals(1042L, item.orderId)
        assertEquals("PENDING", item.status)
        assertEquals(9_300, item.totalAmount)
        assertEquals("아메리카노", item.items?.single()?.menuName)
        assertEquals(2, item.items?.single()?.quantity)
    }

    @Test
    fun myStoreStatusAndCancelDtosKeepOpenApiFields() {
        val store = MyStoreRes(id = 7, name = "강남점", imageUrl = null)
        val status = OrderStatusRes(status = "ACCEPTED")
        val cancel = OrderCancelReq(reason = "품절")

        assertEquals(7L, store.id)
        assertEquals("강남점", store.name)
        assertEquals("ACCEPTED", status.status)
        assertEquals("품절", cancel.reason)
    }
}
