package com.cafeminsu.ui.feature.history

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {
    @Test
    fun mostRecentInProgressOrderIsHighlightedAndPastShowsOnlyDone() {
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
        // 진행중 주문은 가장 최근 1건만 상단 강조, 지난 주문 목록은 완료(DONE)만 노출.
        assertEquals("active-1", content.activeOrder?.id)
        assertEquals("#A-2419", content.activeOrder?.orderNumber)
        assertEquals(listOf("past-1"), content.pastOrders.map { it.id })
        assertEquals("어제 09:00", content.pastOrders.single().dateLabel)
    }

    @Test
    fun onlyMostRecentInProgressOrderIsHighlightedAmongManyActive() {
        val state = listOf(
            sampleOrder("o-71", "EQQD", OrderStatus.Ready, JuneFifthMillis + 3),
            sampleOrder("o-70", "3XHM", OrderStatus.PendingPayment, JuneFifthMillis + 2),
            sampleOrder("o-64", "JLJD", OrderStatus.Completed, JuneFifthMillis + 1),
            sampleOrder("o-51", "2MAQ", OrderStatus.Completed, JuneFourthMillis),
        ).toHistoryUiState(nowMillis = JuneFifthNoonMillis)

        val content = state as HistoryUiState.Content
        // 진행중(READY/PENDING) 중 가장 최근 1건만 강조, 나머지 진행중은 노출 안 함.
        assertEquals("o-71", content.activeOrder?.id)
        // 지난 주문은 DONE 만 최신순.
        assertEquals(listOf("o-64", "o-51"), content.pastOrders.map { it.id })
    }

    @Test
    fun onlyInProgressOrdersStillShowHighlightWithEmptyPastList() {
        val state = listOf(
            sampleOrder("o-71", "EQQD", OrderStatus.Ready, JuneFifthMillis + 1),
            sampleOrder("o-70", "3XHM", OrderStatus.PendingPayment, JuneFifthMillis),
        ).toHistoryUiState(nowMillis = JuneFifthNoonMillis)

        val content = state as HistoryUiState.Content
        // 완료 주문이 없어도 진행중 강조 카드는 노출되고, 지난 주문 목록만 비어 있다.
        assertEquals("o-71", content.activeOrder?.id)
        assertTrue(content.pastOrders.isEmpty())
    }

    @Test
    fun pastOrderUsesServerStoreNameAndFallsBackWhenBlank() {
        val state = listOf(
            sampleOrder(
                id = "past-1",
                orderNumber = "A-1001",
                status = OrderStatus.Completed,
                createdAtMillis = JuneFourthMillis,
                storeName = "민수카페 부산서면점",
            ),
            sampleOrder(
                id = "past-2",
                orderNumber = "A-1002",
                status = OrderStatus.Completed,
                createdAtMillis = JuneFourthMillis,
                storeName = "",
            ),
        ).toHistoryUiState(nowMillis = JuneFifthNoonMillis)

        val content = state as HistoryUiState.Content
        // 서버가 준 실매장명을 그대로 노출하고, 비어 있을 때만 기본값으로 폴백한다(하드코딩 금지).
        assertEquals("민수카페 부산서면점", content.pastOrders.first { it.id == "past-1" }.storeName)
        assertEquals("강남역", content.pastOrders.first { it.id == "past-2" }.storeName)
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
    storeName: String = "",
): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        storeName = storeName,
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
