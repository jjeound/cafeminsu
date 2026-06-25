package com.cafeminsu.data.proximity

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.proximity.ProximityScanner
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * 데모/에뮬레이터용 근접 스캐너. 실물 비콘·권한 없이도 "고객이 매장에 가까워지는" 신호를 만들어
 * 스케줄러를 시연할 수 있게 한다([ProximityScanner] 의 기본 DI 바인딩).
 *
 * [start] 이후 수집되는 동안 도착까지 남은 시간이 점점 줄어드는(가까워지는) 신호를 일정 간격으로 방출하고,
 * 고객이 도착(`0`)하면 흐름을 완료한다. [stop] 하면 더 방출하지 않는다.
 */
@Singleton
class SimulatedProximityScanner @Inject constructor(
    private val clock: Clock,
) : ProximityScanner {
    private val running = MutableStateFlow(false)

    override fun observe(): Flow<AppResult<ProximitySignal>> = flow {
        running.first { it } // start() 전까지 대기(방출 없음).
        var arrivalSeconds = StartArrivalSeconds
        while (running.value && arrivalSeconds >= MinArrivalSeconds) {
            emit(AppResult.Success(signalFor(arrivalSeconds)))
            val next = arrivalSeconds - StepSeconds
            if (next >= MinArrivalSeconds) {
                delay(IntervalMillis)
            }
            arrivalSeconds = next
        }
    }

    override suspend fun start(): AppResult<Unit> {
        running.value = true
        return AppResult.Success(Unit)
    }

    override suspend fun stop() {
        running.value = false
    }

    private fun signalFor(arrivalSeconds: Int): ProximitySignal =
        ProximitySignal(
            orderId = DemoOrderId,
            rssi = rssiFor(arrivalSeconds),
            estimatedArrivalSeconds = arrivalSeconds,
            atMillis = clock.nowMillis(),
        )

    // 가까워질수록(도착초가 줄수록) rssi 가 0 에 가까워진다(강해진다). 순수 정수 보간.
    private fun rssiFor(arrivalSeconds: Int): Int =
        NearRssi - arrivalSeconds * (NearRssi - FarRssi) / StartArrivalSeconds

    private companion object {
        // 시연 대상은 매장 주문 큐의 신규 주문(시드 데이터의 #1042)이다.
        const val DemoOrderId = "owner-order-1042"
        const val StartArrivalSeconds = 90
        const val MinArrivalSeconds = 0
        const val StepSeconds = 30
        const val IntervalMillis = 2_000L
        const val NearRssi = -50
        const val FarRssi = -95
    }
}
