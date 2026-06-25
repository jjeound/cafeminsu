package com.cafeminsu.domain.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import kotlinx.coroutines.runBlocking

/**
 * AI 우선·규칙 폴백 제조시간 추정기. step 0 [PrepTimeEstimator] 계약을 그대로 구현해 DI 교체만으로 활성화된다.
 *
 * [OrderMetricsPredictor.estimatePrepSeconds] 를 우선 시도하고, 모델 미가용/추론 실패([AppResult.Failure])면
 * step 0 규칙 추정기([RulePrepTimeEstimator] 등 [fallback])로 폴백한다. 따라서 모델이 없는 기본 상태에서는
 * 항상 규칙값과 동일하게 동작한다.
 *
 * 동기 계약([estimateSeconds])과 비동기 추정기 사이는 [runBlocking] 으로 잇는다. 기본(모델 미가용) 경로는
 * 추론 없이 즉시 폴백값을 돌려주므로 비용이 작다. 실제 모델 탑재 시 추론 지연이 있으므로 호출 컨텍스트(IO 등)를
 * 고려한다 — AI 는 입력 추정치만 보정하며 [OrderScheduler] 의 결정 로직은 바꾸지 않는다.
 *
 * @param defaultCongestion 동기 계약에는 혼잡도 입력이 없어 추정 컨텍스트로 사용할 기본 혼잡도(결정에는 미관여).
 */
class AiPrepTimeEstimator(
    private val predictor: OrderMetricsPredictor,
    private val fallback: PrepTimeEstimator,
    private val defaultCongestion: CongestionLevel = CongestionLevel.Low,
) : PrepTimeEstimator {
    override fun estimateSeconds(order: Order): Int =
        when (val result = runBlocking { predictor.estimatePrepSeconds(order, defaultCongestion) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> fallback.estimateSeconds(order)
        }
}
