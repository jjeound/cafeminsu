package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.Order

/**
 * 한 주문의 처리 우선순위를 결정하는 입력 신호. 순수 도메인 값으로, 화면/디스패처에 의존하지 않는다.
 *
 * @param waitingSeconds 주문이 대기한 시간(초). 길수록 우선순위가 높아진다(기아 방지).
 * @param prepSeconds 예상 제조 시간(초). ETA 누적과 점수에 사용한다.
 * @param quantity 주문 항목 수량 합.
 * @param congestion 매장 혼잡도(활성 주문 수 기반).
 * @param proximity 근접 신호. 이 step에서는 항상 `null`이며 다음 step의 비콘 신호가 채운다.
 * @param expectedPickupAtMillis 예약/약속 픽업 시각(있을 때만). 마감이 가까울수록 우선순위가 높아진다.
 */
data class SchedulingSignals(
    val orderId: String,
    val waitingSeconds: Long,
    val prepSeconds: Int,
    val quantity: Int,
    val congestion: CongestionLevel,
    val proximity: ProximityInput? = null,
    val expectedPickupAtMillis: Long? = null,
)

/**
 * 고객 근접 신호의 최소 형태. 다음 step의 비콘/BLE 가 이 값을 채운다.
 *
 * @param estimatedArrivalSeconds 예상 도착까지 남은 시간(초). 작을수록 임박.
 * @param rssi 비콘 신호 세기(dBm). 거리 추정 보조값.
 */
data class ProximityInput(
    val estimatedArrivalSeconds: Int,
    val rssi: Int,
)

/** 매장 혼잡도 단계. ordinal 이 점수 가중에 사용되므로 Low < Mid < High 순서를 유지한다. */
enum class CongestionLevel { Low, Mid, High }

/** 주문 카드에 노출하는 우선순위 뱃지. */
enum class SchedulingBadge { ArrivingSoon, Urgent, Normal }

/**
 * 스케줄링 점수·뱃지·ETA 계산에 쓰이는 모든 계수. 로직에 매직넘버를 두지 않기 위해 이름 있는 기본값으로 모은다.
 * 운영 튜닝/실험은 이 값만 바꿔 수행한다.
 *
 * @param waitingWeight 대기 1초당 가산점(양수 — 오래 기다린 주문 우선).
 * @param quantityWeight 수량 1개당 가산점(양수 — 큰 주문 우선).
 * @param prepWeight 제조 1초당 가산점. 빠른 처리량을 위해 긴 제조 주문을 약간 후순위로 두는 음수 기본값.
 * @param proximityWeight 근접 임박도(0..1)에 곱하는 가중치(양수).
 * @param pickupWeight 픽업 마감 임박도(0..1)에 곱하는 가중치(양수).
 * @param congestionWeight 혼잡도 ordinal 에 곱하는 가중치(양수).
 * @param agingThresholdSeconds 이 시간 이상 대기하면 Urgent 뱃지.
 * @param arrivingSoonSeconds 예상 도착이 이 시간 이내면 ArrivingSoon 뱃지·근접 임박도 창.
 * @param prepBaseSeconds 규칙 기반 제조 추정의 기본 시간.
 * @param prepPerItemSeconds 항목당 추가 제조 시간.
 * @param prepPerOptionSeconds 옵션당 추가 제조 시간.
 * @param congestionMidThreshold 활성 주문 수가 이 값 이상이면 Mid.
 * @param congestionHighThreshold 활성 주문 수가 이 값 이상이면 High.
 * @param pickupUrgencyWindowSeconds 픽업 마감 임박도를 0..1 로 환산하는 창(초).
 */
data class SchedulingWeights(
    val waitingWeight: Double = 1.0,
    val quantityWeight: Double = 8.0,
    val prepWeight: Double = -0.1,
    val proximityWeight: Double = 120.0,
    val pickupWeight: Double = 100.0,
    val congestionWeight: Double = 20.0,
    val agingThresholdSeconds: Long = 600L,
    val arrivingSoonSeconds: Int = 120,
    val prepBaseSeconds: Int = 60,
    val prepPerItemSeconds: Int = 30,
    val prepPerOptionSeconds: Int = 10,
    val congestionMidThreshold: Int = 5,
    val congestionHighThreshold: Int = 10,
    val pickupUrgencyWindowSeconds: Long = 600L,
)

/**
 * 스케줄링 결과 한 건. 우선순위 점수와 누적 ETA, 표시 뱃지를 함께 제공한다.
 */
data class ScheduledOrder(
    val order: Order,
    val priorityScore: Double,
    val estimatedReadyAtMillis: Long,
    val badge: SchedulingBadge,
)
