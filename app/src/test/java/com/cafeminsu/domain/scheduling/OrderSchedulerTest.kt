package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderSchedulerTest {
    private val scheduler = OrderScheduler()

    @Test
    fun longerWaitingRanksHigher() {
        val orders = listOf(order("a", createdAtMillis = 2_000L), order("b", createdAtMillis = 1_000L))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 100L),
            "b" to signals("b", waitingSeconds = 500L),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("b", "a"), result.map { it.order.id })
    }

    @Test
    fun largerQuantityRanksHigher() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals("a", quantity = 1),
            "b" to signals("b", quantity = 5),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("b", "a"), result.map { it.order.id })
    }

    @Test
    fun agedOrderRisesToTopAndIsMarkedUrgent() {
        val orders = listOf(order("a"), order("b"), order("c"))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 5_000L, quantity = 1),
            "b" to signals("b", waitingSeconds = 30L, quantity = 6),
            "c" to signals("c", waitingSeconds = 60L, quantity = 4),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "c", "b"), result.map { it.order.id })
        assertEquals(SchedulingBadge.Urgent, result.first().badge)
    }

    @Test
    fun tiedScoresBreakByCreatedAtAscending() {
        val orders = listOf(order("a", createdAtMillis = 5_000L), order("b", createdAtMillis = 1_000L))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 100L, quantity = 1),
            "b" to signals("b", waitingSeconds = 100L, quantity = 1),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("b", "a"), result.map { it.order.id })
    }

    @Test
    fun etaAccumulatesPrepTimesInPriorityOrder() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 500L, prepSeconds = 120),
            "b" to signals("b", waitingSeconds = 100L, prepSeconds = 60),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "b"), result.map { it.order.id })
        assertEquals(NOW + 120_000L, result[0].estimatedReadyAtMillis)
        assertEquals(NOW + 180_000L, result[1].estimatedReadyAtMillis)
    }

    @Test
    fun proximityNullContributesZeroToScore() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 200L, prepSeconds = 100, proximity = null),
            "b" to signals("b", waitingSeconds = 100L, prepSeconds = 100, proximity = null),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "b"), result.map { it.order.id })
        // 200*1.0 + 1*8.0 + 100*(-0.1) + 0(근접) + 0(픽업) + 0(혼잡) = 198.0
        assertEquals(198.0, result.first().priorityScore, EPSILON)
        assertEquals(SchedulingBadge.Normal, result.first().badge)
    }

    @Test
    fun arrivingSoonProximityRanksHigherAndGetsBadge() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals(
                "a",
                waitingSeconds = 50L,
                prepSeconds = 100,
                proximity = ProximityInput(estimatedArrivalSeconds = 30, rssi = -60),
            ),
            "b" to signals("b", waitingSeconds = 50L, prepSeconds = 100, proximity = null),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "b"), result.map { it.order.id })
        // 50 + 8 - 10 + 120*((120-30)/120=0.75)=90 → 138.0
        assertEquals(138.0, result.first().priorityScore, EPSILON)
        assertEquals(SchedulingBadge.ArrivingSoon, result.first().badge)
        assertEquals(SchedulingBadge.Normal, result.last().badge)
    }

    @Test
    fun pickupDeadlineRaisesPriority() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 20L, prepSeconds = 100, expectedPickupAtMillis = NOW),
            "b" to signals("b", waitingSeconds = 20L, prepSeconds = 100),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "b"), result.map { it.order.id })
        // 20 + 8 - 10 + 100*(마감까지 0초 → urgency 1.0)=100 → 118.0
        assertEquals(118.0, result.first().priorityScore, EPSILON)
    }

    @Test
    fun higherCongestionRanksHigher() {
        val orders = listOf(order("a"), order("b"))
        val signals = mapOf(
            "a" to signals("a", waitingSeconds = 50L, prepSeconds = 100, congestion = CongestionLevel.High),
            "b" to signals("b", waitingSeconds = 50L, prepSeconds = 100, congestion = CongestionLevel.Low),
        )

        val result = scheduler.schedule(orders, signals, NOW)

        assertEquals(listOf("a", "b"), result.map { it.order.id })
        // 50 + 8 - 10 + 20*High.ordinal(2)=40 → 88.0
        assertEquals(88.0, result.first().priorityScore, EPSILON)
    }

    @Test
    fun missingSignalFallsBackToEstimatorAndDerivedWaiting() {
        // 신호 맵에 없는 주문: 대기는 createdAt 으로, 제조는 추정기로 보정한다.
        val weights = SchedulingWeights()
        val order = order("a", createdAtMillis = NOW - 700_000L, items = listOf(item(quantity = 1, optionCount = 0)))

        val result = scheduler.schedule(listOf(order), emptyMap(), NOW)

        val expectedPrep = weights.prepBaseSeconds + weights.prepPerItemSeconds // 60 + 30 = 90
        val scheduled = result.single()
        // 700(대기) + 8(수량1*8) + (-0.1*90) = 699.0
        assertEquals(699.0, scheduled.priorityScore, EPSILON)
        assertEquals(NOW + expectedPrep * 1_000L, scheduled.estimatedReadyAtMillis)
        assertEquals(SchedulingBadge.Urgent, scheduled.badge)
    }

    private fun signals(
        orderId: String,
        waitingSeconds: Long = 0L,
        prepSeconds: Int = 100,
        quantity: Int = 1,
        congestion: CongestionLevel = CongestionLevel.Low,
        proximity: ProximityInput? = null,
        expectedPickupAtMillis: Long? = null,
    ): SchedulingSignals =
        SchedulingSignals(
            orderId = orderId,
            waitingSeconds = waitingSeconds,
            prepSeconds = prepSeconds,
            quantity = quantity,
            congestion = congestion,
            proximity = proximity,
            expectedPickupAtMillis = expectedPickupAtMillis,
        )

    private fun order(
        id: String,
        createdAtMillis: Long = 0L,
        items: List<CartItem> = listOf(item(quantity = 1, optionCount = 0)),
    ): Order =
        Order(
            id = id,
            orderNumber = id,
            items = items,
            totalAmount = 4_500,
            status = OrderStatus.Accepted,
            createdAtMillis = createdAtMillis,
        )

    private fun item(quantity: Int, optionCount: Int): CartItem =
        CartItem(
            id = "item-$quantity-$optionCount",
            menuItemId = "americano",
            name = "아메리카노",
            unitPrice = 4_500,
            selectedOptions = (0 until optionCount).map { index ->
                SelectedOption(
                    groupId = "group-$index",
                    optionId = "option-$index",
                    name = "옵션 $index",
                    extraPrice = 0,
                )
            },
            quantity = quantity,
        )

    private companion object {
        const val NOW = 1_750_000_000_000L
        const val EPSILON = 0.0001
    }
}
