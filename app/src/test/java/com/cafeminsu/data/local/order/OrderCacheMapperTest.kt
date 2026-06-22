package com.cafeminsu.data.local.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderCacheMapperTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun roundTripPreservesItemsAndSelectedOptions() {
        val order = Order(
            id = "77",
            orderNumber = "A-2543",
            items = listOf(
                CartItem(
                    id = "cart-item-1",
                    menuItemId = "101",
                    name = "바닐라라떼",
                    unitPrice = 6_000,
                    selectedOptions = listOf(
                        SelectedOption(groupId = "온도", optionId = "1", name = "ICE", extraPrice = 0),
                        SelectedOption(groupId = "샷 추가", optionId = "2", name = "+1샷", extraPrice = 500),
                    ),
                    quantity = 2,
                ),
                CartItem(
                    id = "cart-item-2",
                    menuItemId = "102",
                    name = "아메리카노",
                    unitPrice = 4_500,
                    selectedOptions = emptyList(),
                    quantity = 1,
                ),
            ),
            totalAmount = 13_000,
            status = OrderStatus.Completed,
            createdAtMillis = 1_700_000_000_000L,
        )

        val restored = order.toOrderEntity(moshi).toOrder(moshi)

        assertEquals(order, restored)
    }

    @Test
    fun statusIsStoredAsEnumNameOnEntity() {
        val order = sampleOrder(status = OrderStatus.Accepted)

        val entity = order.toOrderEntity(moshi)

        assertEquals("Accepted", entity.status)
        assertEquals(OrderStatus.Accepted, entity.toOrder(moshi).status)
    }

    @Test
    fun corruptItemsJsonDecodesToEmptyItems() {
        // 캐시 JSON 이 손상돼도 화면 오류로 번지지 않도록 빈 항목으로 흡수한다.
        val entity = OrderEntity(
            id = "78",
            orderNumber = "A-2544",
            totalAmount = 8_500,
            status = "Ready",
            createdAtMillis = 1L,
            itemsJson = "not-json",
        )

        assertEquals(emptyList<CartItem>(), entity.toOrder(moshi).items)
        assertEquals(OrderStatus.Ready, entity.toOrder(moshi).status)
    }

    @Test
    fun unknownStatusStringDecodesToFallbackWithoutCrash() {
        val entity = OrderEntity(
            id = "79",
            orderNumber = "A-2545",
            totalAmount = 0,
            status = "UNKNOWN_STATUS",
            createdAtMillis = 1L,
            itemsJson = "[]",
        )

        assertEquals(OrderStatus.PendingPayment, entity.toOrder(moshi).status)
    }

    private fun sampleOrder(status: OrderStatus): Order =
        Order(
            id = "77",
            orderNumber = "A-2543",
            items = emptyList(),
            totalAmount = 10_000,
            status = status,
            createdAtMillis = 1_700_000_000_000L,
        )
}
