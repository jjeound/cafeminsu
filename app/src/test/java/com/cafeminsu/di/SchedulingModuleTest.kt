package com.cafeminsu.di

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.scheduling.AiPrepTimeEstimator
import com.cafeminsu.domain.scheduling.CongestionLevel
import com.cafeminsu.domain.scheduling.OrderMetricsPredictor
import com.cafeminsu.domain.scheduling.PrepTimeEstimator
import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingWeights
import com.cafeminsu.domain.time.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingModuleTest {
    @Test
    fun providesDefaultWeights() {
        assertEquals(SchedulingWeights(), SchedulingModule.provideSchedulingWeights())
    }

    @Test
    fun providesSystemClock() {
        val clock = SchedulingModule.provideClock()

        assertTrue(clock is SystemClock)
        assertTrue("시스템 시계는 양수 시각을 반환", clock.nowMillis() > 0L)
    }

    @Test
    fun providesRuleEstimatorUsingWeights() {
        val weights = SchedulingModule.provideSchedulingWeights()

        val estimator: PrepTimeEstimator = SchedulingModule.provideRulePrepTimeEstimator(weights)

        assertTrue(estimator is RulePrepTimeEstimator)
    }

    @Test
    fun providesRuleEstimatorAsDefaultPrepTimeEstimator() {
        val weights = SchedulingModule.provideSchedulingWeights()
        val rule = SchedulingModule.provideRulePrepTimeEstimator(weights)

        // 기본 바인딩은 규칙 추정기 — 온디바이스 LLM 은 주문 처리 동기 경로에서 호출하지 않는다(네이티브 크래시 방지).
        val estimator = SchedulingModule.providePrepTimeEstimator(rule)

        assertTrue(estimator is RulePrepTimeEstimator)
    }

    @Test
    fun aiPrepTimeEstimatorFactoryBuildsAiEstimator() {
        val weights = SchedulingModule.provideSchedulingWeights()
        val rule = SchedulingModule.provideRulePrepTimeEstimator(weights)

        // 선택 레이어 팩토리는 AI 우선 추정기를 만든다(기본 그래프엔 미바인딩).
        val estimator = SchedulingModule.aiPrepTimeEstimator(NoopPredictor, rule)

        assertTrue(estimator is AiPrepTimeEstimator)
    }

    private object NoopPredictor : OrderMetricsPredictor {
        override suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int> =
            AppResult.Success(0)

        override suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel> =
            AppResult.Success(CongestionLevel.Low)

        override suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double> =
            AppResult.Success(0.0)
    }
}
