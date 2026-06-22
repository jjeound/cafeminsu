package com.cafeminsu.domain.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.proximity.ProximitySignal

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
