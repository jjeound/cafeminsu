package com.cafeminsu.domain.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.proximity.ProximitySignal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 인터페이스 계약 검증: 추정치는 [AppResult] 로 표현되고(예외 전파 없음), 도착확률은 0.0..1.0 범위를 유지한다.
 */
class OrderMetricsPredictorTest {
    @Test
    fun successAndFailureAreExpressedAsAppResult() = runTest {
        val predictor: OrderMetricsPredictor = StubPredictor(
            prep = AppResult.Success(120),
            congestion = AppResult.Failure(DomainError.Unknown),
        )

        val prep = predictor.estimatePrepSeconds(order(), CongestionLevel.Low)
        val congestion = predictor.predictCongestion(emptyList(), nowMillis = 0L)

        assertEquals(120, (prep as AppResult.Success).data)
        assertTrue(congestion is AppResult.Failure)
    }

    @Test
    fun arrivalProbabilityStaysWithinUnitRange() = runTest {
        val predictor: OrderMetricsPredictor = StubPredictor(arrival = AppResult.Success(0.42))

        val probability = (predictor.estimateArrivalProbability(signal()) as AppResult.Success).data

        assertTrue(probability in 0.0..1.0)
    }

    private fun order(): Order =
        Order(
            id = "o1",
            orderNumber = "1042",
            items = emptyList(),
            totalAmount = 0,
            status = OrderStatus.Accepted,
            createdAtMillis = 0L,
        )

    private fun signal(): ProximitySignal =
        ProximitySignal(orderId = "o1", rssi = -70, estimatedArrivalSeconds = 30, atMillis = 0L)

    private class StubPredictor(
        private val prep: AppResult<Int> = AppResult.Success(0),
        private val congestion: AppResult<CongestionLevel> = AppResult.Success(CongestionLevel.Low),
        private val arrival: AppResult<Double> = AppResult.Success(0.0),
    ) : OrderMetricsPredictor {
        override suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int> = prep

        override suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel> =
            congestion

        override suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double> = arrival
    }
}
