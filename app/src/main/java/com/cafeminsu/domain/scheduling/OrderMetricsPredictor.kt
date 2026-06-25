package com.cafeminsu.domain.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order

/**
 * 한 주문에 대한 고객 근접 신호 한 건(도착확률 보정 입력).
 *
 * 비콘(BLE) 신호원은 제거됐고(phase 29 step0), 현재는 이 AI 보정 레이어의 도착확률 추정 입력 타입으로만 남는다.
 * 보안: 민감정보를 싣지 않고 **주문 참조 ID** 만 둔다. rssi/식별자는 로깅하지 않는다(`docs/SECURITY.md`).
 *
 * @param orderId 신호가 가리키는 주문 식별자(참조 ID).
 * @param rssi 신호 세기(dBm, 보통 음수). 0 에 가까울수록 가깝다(강하다).
 * @param estimatedArrivalSeconds 예상 도착까지 남은 시간(초). 작을수록 임박.
 * @param atMillis 신호 수신 시각(epoch millis).
 */
data class ProximitySignal(
    val orderId: String,
    val rssi: Int,
    val estimatedArrivalSeconds: Int,
    val atMillis: Long,
)

/**
 * 스케줄러 입력값(제조시간·혼잡도·도착확률)을 **보정**하는 온디바이스 AI 추정 계약(선택 레이어).
 *
 * AI 는 의사결정 주체가 아니다 — [OrderScheduler] 의 점수식/정렬은 그대로 두고, 이 인터페이스는 **입력 추정치만**
 * 더 정확히 제공한다. 모델이 없거나 추론이 실패하면 호출측(또는 구현)이 step 0 규칙 추정치로 **자동 폴백**한다.
 *
 * 모든 함수는 추정치만 반환하며 실패는 [AppResult.Failure] 로 표현한다(예외 전파 금지). PII/주문 민감정보는 로깅하지 않는다.
 */
interface OrderMetricsPredictor {
    /**
     * 주문의 예상 제조 시간(초)을 보정 추정한다. 실패 시 호출측이 [RulePrepTimeEstimator] 로 폴백한다.
     *
     * @param congestion 현재 매장 혼잡도(추정 컨텍스트로만 사용, 결정에는 미관여).
     */
    suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int>

    /** 최근 주문 흐름으로 매장 혼잡도([CongestionLevel])를 보정 추정한다. 실패 시 규칙([CongestionCalculator]) 폴백. */
    suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel>

    /** 근접 신호로 고객 도착 확률(0.0..1.0)을 보정 추정한다. 실패 시 규칙(근접 임박도) 폴백. */
    suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double>
}
