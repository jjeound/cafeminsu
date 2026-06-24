package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuSummary
import com.cafeminsu.data.remote.StoreOrderItemRes
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerOrderMapperTest {
    @Test
    fun storeOrderMapsPendingToPaidAndSummaryItems() {
        val result = listOf(
            StoreOrderItemRes(
                orderId = 1042,
                orderNumber = "1042",
                status = "PENDING",
                totalAmount = 9_300,
                items = listOf(
                    MenuSummary(menuId = 101, menuName = "아메리카노", quantity = 2),
                    MenuSummary(menuId = 102, menuName = "바닐라라떼", quantity = 1),
                ),
                createdAt = "2026-06-20T01:15:30Z",
            ),
        ).toOwnerOrders()

        assertTrue(result is AppResult.Success)
        val order = (result as AppResult.Success).data.single()
        assertEquals("1042", order.id)
        assertEquals("1042", order.orderNumber)
        // 점주 큐는 결제 완료된 주문만 들어온다 — 서버 PENDING 을 도메인 Paid 로 매핑한다.
        assertEquals(OrderStatus.Paid, order.status)
        assertEquals(9_300, order.totalAmount)
        assertEquals(1_781_918_130_000, order.createdAtMillis)

        assertEquals(2, order.items.size)
        val first = order.items.first()
        assertEquals("1042-101", first.id)
        assertEquals("101", first.menuItemId)
        assertEquals("아메리카노", first.name)
        // 요약 응답엔 단가가 없어 0 으로 둔다.
        assertEquals(0, first.unitPrice)
        assertEquals(2, first.quantity)
        assertEquals(emptyList<SelectedOption>(), first.selectedOptions)
    }

    @Test
    fun serverStatusesMapToDomainOrderStatus() {
        assertEquals(OrderStatus.Paid, "PENDING".toOwnerOrderStatus())
        assertEquals(OrderStatus.Accepted, "ACCEPTED".toOwnerOrderStatus())
        assertEquals(OrderStatus.Ready, "READY".toOwnerOrderStatus())
        assertEquals(OrderStatus.Completed, "DONE".toOwnerOrderStatus())
        assertEquals(OrderStatus.Cancelled, "CANCELLED".toOwnerOrderStatus())
        assertEquals(null, "UNRECOGNIZED".toOwnerOrderStatus())
    }

    @Test
    fun domainStatusesMapToServerQuery() {
        assertEquals("PENDING", OrderStatus.Paid.toServerOrderStatus())
        assertEquals("ACCEPTED", OrderStatus.Accepted.toServerOrderStatus())
        assertEquals("READY", OrderStatus.Ready.toServerOrderStatus())
        assertEquals("DONE", OrderStatus.Completed.toServerOrderStatus())
        assertEquals("CANCELLED", OrderStatus.Cancelled.toServerOrderStatus())
        // 서버에 대응 상태가 없으면 필터를 보내지 않는다.
        assertEquals(null, OrderStatus.Preparing.toServerOrderStatus())
    }

    @Test
    fun missingOrderIdMapsToUnknownError() {
        val result = listOf(
            StoreOrderItemRes(
                orderId = null,
                orderNumber = "1042",
                status = "ACCEPTED",
                totalAmount = 9_300,
                items = emptyList(),
                createdAt = "2026-06-20T01:15:30Z",
            ),
        ).toOwnerOrders()

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
