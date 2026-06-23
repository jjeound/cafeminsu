package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.Order

/**
 * 우선순위 점수로 주문 처리 순서를 결정하는 순수 도메인 엔진(안드로이드 비종속·결정론적).
 *
 * 단순 선착순(FIFO) 대신 대기시간·수량·제조시간·근접·픽업 마감·혼잡도를 가중합한 점수로 정렬한다.
 * 높은 점수 = 먼저 처리(내림차순), 동점은 `createdAtMillis` 오름차순(먼저 들어온 주문 먼저)으로 안정 정렬한다.
 * DI 는 [com.cafeminsu.di.SchedulingModule] 에서 제공한다.
 */
class OrderScheduler(
    private val weights: SchedulingWeights = SchedulingWeights(),
    private val prepTimeEstimator: PrepTimeEstimator = RulePrepTimeEstimator(weights),
) {
    /**
     * @param orders 정렬 대상 주문.
     * @param signals 주문 id → 입력 신호. 빠진 주문은 안전한 기본 신호(제조=추정기, 대기=createdAt 파생)로 보정한다.
     * @param nowMillis 현재 시각(주입된 Clock 값). 대기/ETA/픽업 임박도 계산 기준.
     */
    fun schedule(
        orders: List<Order>,
        signals: Map<String, SchedulingSignals>,
        nowMillis: Long,
    ): List<ScheduledOrder> {
        val scored = orders.map { order ->
            val signal = signals[order.id] ?: defaultSignals(order, nowMillis)
            ScoredEntry(order = order, signal = signal, score = score(signal, nowMillis))
        }

        val ordered = scored.sortedWith(
            compareByDescending<ScoredEntry> { it.score }
                .thenBy { it.order.createdAtMillis },
        )

        // ETA = now + (앞 순번들의 prepSeconds 합 + 본인 prepSeconds) * 1000.
        var cumulativePrepSeconds = 0L
        return ordered.map { entry ->
            cumulativePrepSeconds += entry.signal.prepSeconds
            ScheduledOrder(
                order = entry.order,
                priorityScore = entry.score,
                estimatedReadyAtMillis = nowMillis + cumulativePrepSeconds * MILLIS_PER_SECOND,
                badge = badge(entry.signal),
            )
        }
    }

    private fun score(signals: SchedulingSignals, nowMillis: Long): Double =
        weights.waitingWeight * signals.waitingSeconds +
            weights.quantityWeight * signals.quantity +
            weights.prepWeight * signals.prepSeconds +
            weights.proximityWeight * proximityUrgency(signals.proximity) +
            weights.pickupWeight * pickupUrgency(signals.expectedPickupAtMillis, nowMillis) +
            weights.congestionWeight * signals.congestion.ordinal

    /**
     * 근접 임박도(0..1). 근접 신호가 없으면 0(기여 없음 — 이 step 에서는 항상 null).
     * 도착이 임박할수록 1.0 에 가까운 단조감소: `(arrivingSoonSeconds - estimatedArrivalSeconds)/arrivingSoonSeconds`.
     * 창(arrivingSoonSeconds) 밖이면 분자가 음수가 되어 0 으로 clamp 된다.
     */
    private fun proximityUrgency(proximity: ProximityInput?): Double {
        proximity ?: return 0.0
        val window = weights.arrivingSoonSeconds.toDouble()
        val urgency = (weights.arrivingSoonSeconds - proximity.estimatedArrivalSeconds) / window
        return urgency.coerceIn(0.0, 1.0)
    }

    /**
     * 픽업 마감 임박도(0..1). 약속 픽업 시각이 없으면 0.
     * 마감까지 남은 시간이 적을수록 1.0 에 가까워지며, 이미 지났으면 1.0 으로 clamp.
     */
    private fun pickupUrgency(expectedPickupAtMillis: Long?, nowMillis: Long): Double {
        val pickupAt = expectedPickupAtMillis ?: return 0.0
        val window = weights.pickupUrgencyWindowSeconds.toDouble()
        val remainingSeconds = (pickupAt - nowMillis).toDouble() / MILLIS_PER_SECOND
        val urgency = (window - remainingSeconds) / window
        return urgency.coerceIn(0.0, 1.0)
    }

    private fun badge(signals: SchedulingSignals): SchedulingBadge {
        val proximity = signals.proximity
        return when {
            proximity != null && proximity.estimatedArrivalSeconds <= weights.arrivingSoonSeconds ->
                SchedulingBadge.ArrivingSoon

            signals.waitingSeconds >= weights.agingThresholdSeconds -> SchedulingBadge.Urgent
            else -> SchedulingBadge.Normal
        }
    }

    private fun defaultSignals(order: Order, nowMillis: Long): SchedulingSignals =
        SchedulingSignals(
            orderId = order.id,
            waitingSeconds = ((nowMillis - order.createdAtMillis) / MILLIS_PER_SECOND).coerceAtLeast(0L),
            prepSeconds = prepTimeEstimator.estimateSeconds(order),
            quantity = order.items.sumOf { it.quantity },
            congestion = CongestionLevel.Low,
            proximity = null,
            expectedPickupAtMillis = null,
        )

    private data class ScoredEntry(
        val order: Order,
        val signal: SchedulingSignals,
        val score: Double,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
