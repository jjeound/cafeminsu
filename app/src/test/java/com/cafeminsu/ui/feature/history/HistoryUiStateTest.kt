package com.cafeminsu.ui.feature.history

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {
    @Test
    fun ordersAreSeparatedIntoCurrentOrderAndPastOrders() {
        val state = listOf(
            sampleOrder(
                id = "past-1",
                orderNumber = "A-1001",
                status = OrderStatus.Completed,
                createdAtMillis = JuneFourthMillis,
            ),
            sampleOrder(
                id = "active-1",
                orderNumber = "A-2419",
                status = OrderStatus.Preparing,
                createdAtMillis = JuneFifthMillis,
            ),
        ).toHistoryUiState(nowMillis = JuneFifthNoonMillis)

        assertTrue(state is HistoryUiState.Content)
        val content = state as HistoryUiState.Content
        assertEquals("active-1", content.activeOrder?.id)
        assertEquals("#A-2419", content.activeOrder?.orderNumber)
        assertEquals("바닐라라떼 외 1개", content.activeOrder?.itemSummary)
        assertEquals("10,000원", content.activeOrder?.amountLabel)
        assertEquals(listOf("past-1"), content.pastOrders.map { it.id })
        assertEquals("어제 09:00", content.pastOrders.single().dateLabel)
    }

    @Test
    fun emptyOrdersMapToEmptyState() {
        val state = emptyList<Order>().toHistoryUiState(nowMillis = JuneFifthNoonMillis)

        assertEquals(
            HistoryUiState.Empty(
                title = "아직 주문 내역이 없어요",
                message = "첫 번째 한 잔을 주문해보세요",
            ),
            state,
        )
    }

    @Test
    fun orderStatusMapsToFourHistorySteps() {
        val preparingSteps = historyOrderSteps(OrderStatus.Preparing)

        assertEquals(listOf("접수", "수락", "준비중", "완료"), preparingSteps.map { it.label })
        assertEquals(HistoryStepState.Completed, preparingSteps[0].state)
        assertEquals(HistoryStepState.Completed, preparingSteps[1].state)
        assertEquals(HistoryStepState.Current, preparingSteps[2].state)
        assertEquals(HistoryStepState.Upcoming, preparingSteps[3].state)

        val acceptedSteps = historyOrderSteps(OrderStatus.Accepted)
        assertEquals(HistoryStepState.Completed, acceptedSteps[0].state)
        assertEquals(HistoryStepState.Current, acceptedSteps[1].state)

        val readySteps = historyOrderSteps(OrderStatus.Ready)
        assertEquals(HistoryStepState.Completed, readySteps[2].state)
        assertEquals(HistoryStepState.Current, readySteps[3].state)
    }
}

private fun sampleOrder(
    id: String,
    orderNumber: String,
    status: OrderStatus,
    createdAtMillis: Long,
): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        items = listOf(
            CartItem(
                id = "$id-item-1",
                menuItemId = "vanilla-latte",
                name = "바닐라라떼",
                unitPrice = 5_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
            CartItem(
                id = "$id-item-2",
                menuItemId = "americano",
                name = "아메리카노",
                unitPrice = 4_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 10_000,
        status = status,
        createdAtMillis = createdAtMillis,
    )

private const val JuneFourthMillis = 1_780_531_200_000L
private const val JuneFifthMillis = 1_780_617_600_000L
private const val JuneFifthNoonMillis = 1_780_628_400_000L
