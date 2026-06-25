package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.ItemRes
import com.cafeminsu.data.remote.OptionRes
import com.cafeminsu.data.remote.OrderCreateRes
import com.cafeminsu.data.remote.OrderDetailRes
import com.cafeminsu.data.remote.OrderListItemRes
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMapperTest {
    @Test
    fun createResponseUsesServerTotalAmountAndMapsPendingStatus() {
        val result = OrderCreateRes(
            orderId = 77,
            orderNumber = "A-2543",
            totalAmount = 13_000,
            status = "PENDING",
        ).toOrder(
            cartItems = sampleCartItems(),
            createdAtMillis = 1_780_000_000_000,
        )

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data
        assertEquals("77", order.id)
        assertEquals("A-2543", order.orderNumber)
        assertEquals(13_000, order.totalAmount)
        assertEquals(OrderStatus.PendingPayment, order.status)
        assertEquals(sampleCartItems(), order.items)
        assertEquals(1_780_000_000_000, order.createdAtMillis)
    }

    @Test
    fun detailResponseMapsItemsAndIsoCreatedAt() {
        val result = OrderDetailRes(
            orderId = 77,
            orderNumber = "A-2543",
            storeId = 11,
            storeName = "카페민수 강남점",
            orderType = "MOBILE",
            orderMethod = "MANUAL",
            status = "READY",
            totalAmount = 10_000,
            cancelReason = null,
            items = listOf(
                ItemRes(
                    menuId = 101,
                    menuName = "바닐라라떼",
                    quantity = 1,
                    unitPrice = 5_500,
                    options = listOf(
                        OptionRes(
                            optionId = 1,
                            optionGroup = "온도",
                            optionName = "ICE",
                            optionPrice = 0,
                        ),
                    ),
                    subtotal = 5_500,
                ),
            ),
            payment = null,
            createdAt = "2026-06-20T01:15:30Z",
        ).toOrder()

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data
        assertEquals("77", order.id)
        assertEquals(OrderStatus.Ready, order.status)
        assertEquals(1_781_918_130_000, order.createdAtMillis)
        assertEquals("101", order.items.single().menuItemId)
        assertEquals("바닐라라떼", order.items.single().name)
        assertEquals("1", order.items.single().selectedOptions.single().optionId)
    }

    @Test
    fun historyItemWithZonelessCreatedAtMapsToNonZeroMillis() {
        val expected = java.time.LocalDateTime.parse("2026-06-20T10:15:30")
            .atZone(java.time.ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        val result = listOf(
            OrderListItemRes(
                orderId = 77,
                orderNumber = "A-2543",
                storeName = "카페민수 강남점",
                totalAmount = 10_000,
                status = "DONE",
                createdAt = "2026-06-20T10:15:30",
            ),
        ).toOrders()

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data.single()
        assertTrue(order.createdAtMillis > 0L)
        assertEquals(expected, order.createdAtMillis)
    }

    @Test
    fun historyItemMapsWithoutItemDetails() {
        val result = listOf(
            OrderListItemRes(
                orderId = 77,
                orderNumber = "A-2543",
                storeName = "카페민수 강남점",
                totalAmount = 10_000,
                status = "DONE",
                createdAt = "2026-06-20T01:15:30Z",
            ),
        ).toOrders()

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data.single()
        assertEquals("77", order.id)
        assertEquals(OrderStatus.Completed, order.status)
        assertEquals(10_000, order.totalAmount)
        assertEquals(emptyList<com.cafeminsu.domain.model.CartItem>(), order.items)
    }

    @Test
    fun missingOrderIdMapsToUnknownError() {
        val result = OrderCreateRes(
            orderId = null,
            orderNumber = "A-2543",
            totalAmount = 10_000,
            status = "PENDING",
        ).toOrder(
            cartItems = sampleCartItems(),
            createdAtMillis = 1_780_000_000_000,
        )

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }

    private fun sampleCartItems(): List<CartItem> =
        listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "101",
                name = "바닐라라떼",
                unitPrice = 6_000,
                selectedOptions = listOf(
                    SelectedOption(
                        groupId = "온도",
                        optionId = "1",
                        name = "ICE",
                        extraPrice = 0,
                    ),
                    SelectedOption(
                        groupId = "샷 추가",
                        optionId = "2",
                        name = "+1샷",
                        extraPrice = 500,
                    ),
                ),
                quantity = 2,
            ),
        )
}
