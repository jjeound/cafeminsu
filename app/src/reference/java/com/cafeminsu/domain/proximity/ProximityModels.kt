package com.cafeminsu.domain.proximity

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.scheduling.ProximityInput
import kotlinx.coroutines.flow.Flow

/**
 * 한 주문에 대한 고객 근접 신호 한 건. 비콘(BLE) 스캔 또는 시뮬레이터가 생성한다.
 *
 * 보안: 비콘에는 민감정보를 탑재하지 않고 **주문 참조 ID** 만 싣는다. rssi/식별자는 로깅하지 않는다(`docs/SECURITY.md §9`).
 *
 * @param orderId 신호가 가리키는 주문 식별자(참조 ID).
 * @param rssi 비콘 신호 세기(dBm, 보통 음수). 0 에 가까울수록 가깝다(강하다).
 * @param estimatedArrivalSeconds 예상 도착까지 남은 시간(초). 작을수록 임박.
 * @param atMillis 신호 수신 시각(epoch millis).
 */
data class ProximitySignal(
    val orderId: String,
    val rssi: Int,
    val estimatedArrivalSeconds: Int,
    val atMillis: Long,
)

/**
 * 근접 신호 소스 추상화. 실(BLE) 구현([com.cafeminsu.data.proximity.AndroidBeaconScanner])과
 * 시뮬레이터([com.cafeminsu.data.proximity.SimulatedProximityScanner])를 DI 로 교체한다.
 *
 * 권한/하드웨어 부재 등 실패는 예외를 던지지 않고 [AppResult.Failure] 또는 빈 흐름으로 안전 처리한다(크래시 금지).
 */
interface ProximityScanner {
    /** 근접 신호 스트림. 수집이 시작되고 [start] 로 활성화된 동안만 방출하며, 수집이 취소되면 멈춘다. */
    fun observe(): Flow<AppResult<ProximitySignal>>

    /** 스캔 활성화. 권한/하드웨어 부재 시 [AppResult.Failure] 를 반환(폴백 신호). 멱등. */
    suspend fun start(): AppResult<Unit>

    /** 스캔 비활성화. 자원을 해제한다. 멱등. */
    suspend fun stop()
}

/**
 * 근접 신호(도메인) → 스케줄러 입력([ProximityInput]) 매핑. 순수 함수.
 * 음수 도착초(이미 도착)는 0 으로 보정해 스케줄러가 안전하게 처리하도록 한다(입력 검증).
 */
fun ProximitySignal.toProximityInput(): ProximityInput =
    ProximityInput(
        estimatedArrivalSeconds = estimatedArrivalSeconds.coerceAtLeast(0),
        rssi = rssi,
    )
