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
 * 제조시간 추정기([PrepTimeEstimator]) 는 기본으로 **규칙 추정기**([RulePrepTimeEstimator]) 를 바인딩한다.
 * AI 보정([AiPrepTimeEstimator] → [OrderMetricsPredictor] → [GemmaOrderMetricsPredictor]) 은 **선택 레이어**다.
 * 온디바이스 LLM(MediaPipe) 네이티브 추론을 주문 처리 동기 경로에서 호출하면 프로세스를 죽일 수 있고(네이티브
 * 크래시는 try/catch 로 못 막는다) `runBlocking` 으로 스레드를 막는 문제도 있어 **기본 그래프엔 바인딩하지 않는다**.
 * AI 를 켜려면 [providePrepTimeEstimator] 바인딩을 [aiPrepTimeEstimator] 로 교체한다(전용 IO 컨텍스트·검증된 모델 전제).
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

    /**
     * 기본 제조시간 추정기 = **규칙**. AI 보정은 [aiPrepTimeEstimator] 로 분리(기본 미바인딩) — 모듈 KDoc 참고.
     */
    @Provides
    fun providePrepTimeEstimator(ruleEstimator: RulePrepTimeEstimator): PrepTimeEstimator = ruleEstimator

    /**
     * 선택: AI 우선·규칙 폴백 추정기 팩토리. 기본 그래프엔 바인딩하지 않는다(온디바이스 LLM 네이티브 안정성 이슈).
     * 활성화하려면 [providePrepTimeEstimator] 대신 이 결과를 `@Provides` 로 노출한다.
     */
    fun aiPrepTimeEstimator(
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
