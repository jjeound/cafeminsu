package com.cafeminsu.di

import com.cafeminsu.domain.scheduling.CongestionCalculator
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
 * 추정기는 규칙 기반 구현을 제공하되, 다음 step 의 AI 추정기로 교체 가능하다.
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
    fun providePrepTimeEstimator(weights: SchedulingWeights): PrepTimeEstimator =
        RulePrepTimeEstimator(weights)

    @Provides
    fun provideCongestionCalculator(weights: SchedulingWeights): CongestionCalculator =
        CongestionCalculator(weights)

    @Provides
    fun provideOrderScheduler(
        weights: SchedulingWeights,
        prepTimeEstimator: PrepTimeEstimator,
    ): OrderScheduler = OrderScheduler(weights = weights, prepTimeEstimator = prepTimeEstimator)
}
