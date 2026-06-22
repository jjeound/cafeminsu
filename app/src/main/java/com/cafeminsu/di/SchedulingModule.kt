package com.cafeminsu.di

import com.cafeminsu.data.scheduling.GemmaOrderMetricsPredictor
import com.cafeminsu.domain.scheduling.AiPrepTimeEstimator
import com.cafeminsu.domain.scheduling.CongestionCalculator
import com.cafeminsu.domain.scheduling.OrderMetricsPredictor
import com.cafeminsu.domain.scheduling.OrderScheduler
import com.cafeminsu.domain.scheduling.PrepTimeEstimator
import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingWeights
import com.cafeminsu.domain.time.Clock
import com.cafeminsu.domain.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 스케줄링 도메인 의존성 제공. 엔진/계산기는 순수 도메인 클래스(생성자 기본값 보유)라 모듈에서 명시적으로 조립한다.
 *
 * 제조시간 추정기([PrepTimeEstimator]) 는 **AI 우선·규칙 폴백**([AiPrepTimeEstimator]) 로 바인딩한다. AI 추정기
 * ([OrderMetricsPredictor] → [GemmaOrderMetricsPredictor]) 가 미가용/실패하면 규칙 추정기([RulePrepTimeEstimator])
 * 로 자동 폴백하므로, **모델이 없는 기본 상태에서도 규칙값과 동일하게 정상 동작**한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object SchedulingModule {
    @Provides
    @Singleton
    fun provideSchedulingWeights(): SchedulingWeights = SchedulingWeights()

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    fun provideRulePrepTimeEstimator(weights: SchedulingWeights): RulePrepTimeEstimator =
        RulePrepTimeEstimator(weights)

    @Provides
    fun provideOrderMetricsPredictor(predictor: GemmaOrderMetricsPredictor): OrderMetricsPredictor = predictor

    @Provides
    fun providePrepTimeEstimator(
        predictor: OrderMetricsPredictor,
        ruleFallback: RulePrepTimeEstimator,
    ): PrepTimeEstimator = AiPrepTimeEstimator(predictor = predictor, fallback = ruleFallback)

    @Provides
    fun provideCongestionCalculator(weights: SchedulingWeights): CongestionCalculator =
        CongestionCalculator(weights)

    @Provides
    fun provideOrderScheduler(
        weights: SchedulingWeights,
        prepTimeEstimator: PrepTimeEstimator,
    ): OrderScheduler = OrderScheduler(weights = weights, prepTimeEstimator = prepTimeEstimator)
}
