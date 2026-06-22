package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingModelsTest {
    @Test
    fun defaultWeightsHaveExpectedSigns() {
        val weights = SchedulingWeights()

        // 대기·수량은 우선순위를 높이는 방향(양수)이어야 한다.
        assertTrue("대기 가중치는 양수", weights.waitingWeight > 0.0)
        assertTrue("수량 가중치는 양수", weights.quantityWeight > 0.0)
        assertTrue("근접 가중치는 양수", weights.proximityWeight > 0.0)
        assertTrue("픽업 가중치는 양수", weights.pickupWeight > 0.0)
        assertTrue("혼잡 가중치는 양수", weights.congestionWeight > 0.0)
        assertTrue("기아 방지 임계값은 양수", weights.agingThresholdSeconds > 0L)
        assertTrue("곧 도착 임계값은 양수", weights.arrivingSoonSeconds > 0)
        assertTrue("제조 기본 시간은 양수", weights.prepBaseSeconds > 0)
        assertTrue("항목당 제조 시간은 양수", weights.prepPerItemSeconds > 0)
        assertTrue("옵션당 제조 시간은 양수", weights.prepPerOptionSeconds > 0)
        assertTrue("혼잡 Mid 임계값 < High 임계값", weights.congestionMidThreshold < weights.congestionHighThreshold)
    }

    @Test
    fun congestionLevelsAreOrderedLowToHigh() {
        assertTrue(CongestionLevel.Low.ordinal < CongestionLevel.Mid.ordinal)
        assertTrue(CongestionLevel.Mid.ordinal < CongestionLevel.High.ordinal)
    }

    @Test
    fun schedulingSignalsDefaultsHaveNoProximityOrPickup() {
        val signals = SchedulingSignals(
            orderId = "o1",
            waitingSeconds = 10L,
            prepSeconds = 90,
            quantity = 1,
            congestion = CongestionLevel.Low,
        )

        assertNull(signals.proximity)
        assertNull(signals.expectedPickupAtMillis)
    }

    @Test
    fun scheduledOrderHoldsScoreEtaAndBadge() {
        val order = Order(
            id = "o1",
            orderNumber = "1042",
            items = listOf(
                CartItem(
                    id = "o1-item",
                    menuItemId = "americano",
                    name = "아메리카노",
                    unitPrice = 4_500,
                    selectedOptions = emptyList(),
                    quantity = 1,
                ),
            ),
            totalAmount = 4_500,
            status = OrderStatus.Accepted,
            createdAtMillis = 1_000L,
        )
        val scheduled = ScheduledOrder(
            order = order,
            priorityScore = 12.5,
            estimatedReadyAtMillis = 2_000L,
            badge = SchedulingBadge.Urgent,
        )

        assertEquals(order, scheduled.order)
        assertEquals(12.5, scheduled.priorityScore, 0.0)
        assertEquals(2_000L, scheduled.estimatedReadyAtMillis)
        assertEquals(SchedulingBadge.Urgent, scheduled.badge)
    }
}
