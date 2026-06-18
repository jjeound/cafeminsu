package com.cafeminsu.ui.feature.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderStatusUiStateTest {
    @Test
    fun orderMapsToContentUiState() {
        val order = sampleOrder(status = OrderStatus.Accepted)

        val content = order.toOrderStatusContent()

        assertEquals("order-1", content.orderId)
        assertEquals("M042", content.orderNumber)
        assertEquals(OrderStatus.Accepted, content.status)
        assertEquals("주문이 들어갔어요", content.headerTitle)
        assertEquals("매장에서 주문을 확인했어요.", content.statusMessage)
        assertEquals(12_000, content.totalAmount)
        assertEquals(listOf("민수 라떼"), content.items.map { it.name })
        assertEquals(OrderStatusStepState.Completed, content.steps.stepFor(OrderStatus.Paid).state)
        assertEquals(OrderStatusStepState.Current, content.steps.stepFor(OrderStatus.Accepted).state)
    }

    @Test
    fun orderStatusLabelsAndStepProgressAreMappedForDisplay() {
        assertEquals("결제 확인 중", OrderStatus.PendingPayment.orderStatusLabel())
        assertEquals("결제 완료", OrderStatus.Paid.orderStatusLabel())
        assertEquals("주문 접수", OrderStatus.Accepted.orderStatusLabel())
        assertEquals("준비 중", OrderStatus.Preparing.orderStatusLabel())
        assertEquals("픽업 준비 완료", OrderStatus.Ready.orderStatusLabel())
        assertEquals("픽업 완료", OrderStatus.Completed.orderStatusLabel())
        assertEquals("주문 취소", OrderStatus.Cancelled.orderStatusLabel())
        assertEquals("주문 실패", OrderStatus.Failed.orderStatusLabel())

        val readySteps = orderStatusSteps(OrderStatus.Ready)
        assertEquals(OrderStatusStepState.Completed, readySteps.stepFor(OrderStatus.Preparing).state)
        assertEquals(OrderStatusStepState.Current, readySteps.stepFor(OrderStatus.Ready).state)
        assertEquals(OrderStatusStepState.Upcoming, readySteps.stepFor(OrderStatus.Completed).state)

        val failedSteps = orderStatusSteps(OrderStatus.Failed)
        assertEquals(listOf(OrderStatus.Failed), failedSteps.map { it.status })
        assertEquals(OrderStatusStepState.Current, failedSteps.single().state)
    }
}

private fun List<OrderStatusStepUiModel>.stepFor(status: OrderStatus): OrderStatusStepUiModel =
    first { it.status == status }

private fun sampleOrder(
    status: OrderStatus,
): Order =
    Order(
        id = "order-1",
        orderNumber = "M042",
        items = listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "latte",
                name = "민수 라떼",
                unitPrice = 6_000,
                selectedOptions = emptyList(),
                quantity = 2,
            ),
        ),
        totalAmount = 12_000,
        status = status,
        createdAtMillis = 1_800_000_000_000L,
    )
