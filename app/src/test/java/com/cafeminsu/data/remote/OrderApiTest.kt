package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderApiTest {
    @Test
    fun createRequestKeepsOpenApiFields() {
        val request = OrderCreateReq(
            storeId = 11,
            orderType = "MOBILE",
            orderMethod = "MANUAL",
            items = listOf(
                Item(
                    menuId = 101,
                    quantity = 2,
                    optionIds = listOf(1, 2),
                ),
            ),
        )

        assertEquals(11L, request.storeId)
        assertEquals("MOBILE", request.orderType)
        assertEquals("MANUAL", request.orderMethod)
        assertEquals(listOf(1L, 2L), request.items.single().optionIds)
    }

    @Test
    fun orderResponseDtosKeepOpenApiFields() {
        val detail = OrderDetailRes(
            orderId = 77,
            orderNumber = "A-2543",
            storeId = 11,
            storeName = "카페민수 강남점",
            orderType = "MOBILE",
            orderMethod = "MANUAL",
            status = "ACCEPTED",
            totalAmount = 10_000,
            cancelReason = null,
            items = listOf(
                ItemRes(
                    menuId = 101,
                    menuName = "바닐라라떼",
                    quantity = 1,
                    unitPrice = 5_500,
                    options = emptyList(),
                    subtotal = 5_500,
                ),
            ),
            payment = null,
            createdAt = "2026-06-20T01:15:30Z",
        )

        assertEquals(77L, detail.orderId)
        assertEquals("ACCEPTED", detail.status)
        assertEquals("바닐라라떼", detail.items?.single()?.menuName)
    }
}
