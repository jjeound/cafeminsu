package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.Order

/**
 * 주문의 예상 제조 시간(초)을 추정하는 도메인 계약.
 *
 * 다음 step의 AI(학습 기반) 추정기가 이 인터페이스를 구현/대체한다. 추정은 순수 함수이며 결정론적이다.
 */
interface PrepTimeEstimator {
    fun estimateSeconds(order: Order): Int
}

/**
 * 규칙 기반 제조시간 추정기.
 *
 * `prepBaseSeconds + Σ(prepPerItemSeconds + prepPerOptionSeconds * 옵션수) * quantity`.
 * 항목 수·수량·옵션 수에 대해 단조 증가한다. DI 는 [com.cafeminsu.di.SchedulingModule] 에서 제공한다.
 */
class RulePrepTimeEstimator(
    private val weights: SchedulingWeights = SchedulingWeights(),
) : PrepTimeEstimator {
    override fun estimateSeconds(order: Order): Int =
        weights.prepBaseSeconds +
            order.items.sumOf { item ->
                (weights.prepPerItemSeconds + weights.prepPerOptionSeconds * item.selectedOptions.size) * item.quantity
            }
}
