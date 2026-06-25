package com.cafeminsu.domain.scheduling

/**
 * 활성 주문(접수+준비중) 수를 [CongestionLevel]로 환산한다. 경계값은 [SchedulingWeights]로만 둔다(매직넘버 금지).
 * DI 는 [com.cafeminsu.di.SchedulingModule] 에서 제공한다.
 */
class CongestionCalculator(
    private val weights: SchedulingWeights = SchedulingWeights(),
) {
    fun level(activeOrderCount: Int): CongestionLevel =
        when {
            activeOrderCount >= weights.congestionHighThreshold -> CongestionLevel.High
            activeOrderCount >= weights.congestionMidThreshold -> CongestionLevel.Mid
            else -> CongestionLevel.Low
        }
}
