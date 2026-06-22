package com.cafeminsu.domain.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.proximity.ProximitySignal
import org.junit.Assert.assertEquals
import org.junit.Test

class AiPrepTimeEstimatorTest {
    private val weights = SchedulingWeights()
    private val rule = RulePrepTimeEstimator(weights)

    @Test
    fun usesPredictorEstimateWhenSuccess() {
        val estimator = AiPrepTimeEstimator(
            predictor = FakePredictor(prepResult = AppResult.Success(999)),
            fallback = rule,
        )

        assertEquals(999, estimator.estimateSeconds(order()))
    }

    @Test
    fun fallsBackToRuleWhenPredictorFails() {
        val order = order()
        val estimator = AiPrepTimeEstimator(
            predictor = FakePredictor(prepResult = AppResult.Failure(DomainError.Unknown)),
            fallback = rule,
        )

        assertEquals(rule.estimateSeconds(order), estimator.estimateSeconds(order))
    }

    private fun order(): Order =
        Order(
            id = "o1",
            orderNumber = "1042",
            items = listOf(
                CartItem(
                    id = "item-1",
                    menuItemId = "americano",
                    name = "아메리카노",
                    unitPrice = 4_500,
                    selectedOptions = listOf(
                        SelectedOption(groupId = "g", optionId = "iced", name = "ICE", extraPrice = 0),
                    ),
                    quantity = 1,
                ),
            ),
            totalAmount = 4_500,
            status = OrderStatus.Accepted,
            createdAtMillis = 0L,
        )

    private class FakePredictor(
        private val prepResult: AppResult<Int>,
    ) : OrderMetricsPredictor {
        override suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int> =
            prepResult

        override suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel> =
            AppResult.Success(CongestionLevel.Low)

        override suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double> =
            AppResult.Success(0.0)
    }
}
