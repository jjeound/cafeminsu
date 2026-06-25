package com.cafeminsu.data.proximity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.proximity.ProximityScanner
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/** rssi→도착초 매핑·권한 가드의 안드로이드 비종속 경계값. 매직넘버 대신 이름 있는 상수로 둔다. */
internal const val MinBeaconArrivalSeconds = 0

/** 가장 약한 신호일 때의 도착초 상한(스케줄러의 ArrivingSoon 창과 동일하게 둔다). */
internal const val MaxBeaconArrivalSeconds = 120

// rssi(dBm) 거리 추정 경계. 0 에 가까울수록 강하다(가깝다). 이보다 강하면 도착 임박, 약하면 멀다고 본다.
private const val NearestRssi = -50
private const val FarthestRssi = -95

/**
 * 비콘 신호 세기(rssi)로 예상 도착 시간(초)을 추정하는 순수 함수.
 * 신호가 강할수록(0 에 가까울수록) 가까워 도착초가 작다. 범위 밖은 [MinBeaconArrivalSeconds]·
 * [MaxBeaconArrivalSeconds] 로 clamp 해 스케줄러가 안전하게 처리하도록 한다(입력 검증).
 */
internal fun estimateArrivalSecondsFromRssi(rssi: Int): Int {
    val clampedRssi = rssi.coerceIn(FarthestRssi, NearestRssi)
    val span = (NearestRssi - FarthestRssi).toDouble()
    val distanceFromNearest = (NearestRssi - clampedRssi).toDouble()
    val fraction = distanceFromNearest / span
    val range = MaxBeaconArrivalSeconds - MinBeaconArrivalSeconds
    return MinBeaconArrivalSeconds + (fraction * range).toInt()
}

/**
 * SDK 버전별 BLE 스캔에 필요한 런타임 권한 목록(최소 범위).
 * SDK 31(S)+ 는 `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, 그 이하는 위치 권한으로 스캔한다(`docs/SECURITY.md`).
 */
internal fun requiredBluetoothScanPermissions(sdkInt: Int): List<String> =
    if (sdkInt >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/**
 * 안드로이드 내장 [BluetoothLeScanner] 로 매장 근처 고객 비콘을 스캔하는 실(real) [ProximityScanner].
 *
 * 외부 BLE 라이브러리 없이 플랫폼 API 만 사용한다. 권한 미허용·블루투스 비활성·하드웨어 부재는 예외를 던지지
 * 않고 [AppResult.Failure] 로 안전 처리하며(크래시 금지), 거부 시 호출부는 시뮬레이터/근접 없이 폴백한다.
 *
 * 보안: 비콘에는 민감정보를 싣지 않고 **주문 참조 ID** 만 광고한다(`scanRecord.deviceName`).
 * rssi·식별자·주문 id 는 로깅하지 않는다(`docs/SECURITY.md §9`).
 *
 * 기본 DI 바인딩은 에뮬레이터/CI 안전한 시뮬레이터다. 실 BLE 로 교체하는 방법은 [com.cafeminsu.di.ProximityModule].
 */
@Singleton
class AndroidBeaconScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : ProximityScanner {
    private val signals = MutableSharedFlow<AppResult<ProximitySignal>>(
        extraBufferCapacity = SignalBufferCapacity,
    )
    private var leScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    override fun observe(): Flow<AppResult<ProximitySignal>> = signals.asSharedFlow()

    @SuppressLint("MissingPermission") // 권한은 hasScanPermission() 으로 런타임 확인 후에만 스캔한다.
    override suspend fun start(): AppResult<Unit> = withContext(dispatcher) {
        if (!hasScanPermission()) {
            return@withContext AppResult.Failure(DomainError.Validation("bluetooth_permission"))
        }
        val scanner = bluetoothLeScannerOrNull()
            ?: return@withContext AppResult.Failure(DomainError.Validation("bluetooth_unavailable"))

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val signal = result?.toProximitySignalOrNull() ?: return
                signals.tryEmit(AppResult.Success(signal))
            }

            override fun onScanFailed(errorCode: Int) {
                signals.tryEmit(AppResult.Failure(DomainError.Unknown))
            }
        }

        val started = runCatching { scanner.startScan(callback) }.isSuccess
        if (!started) {
            return@withContext AppResult.Failure(DomainError.Unknown)
        }
        leScanner = scanner
        scanCallback = callback
        AppResult.Success(Unit)
    }

    @SuppressLint("MissingPermission") // stopScan 은 시작과 동일 권한 컨텍스트에서만 호출된다.
    override suspend fun stop() {
        withContext(dispatcher) {
            scanCallback?.let { callback ->
                runCatching { leScanner?.stopScan(callback) }
            }
            scanCallback = null
            leScanner = null
        }
    }

    private fun ScanResult.toProximitySignalOrNull(): ProximitySignal? {
        // 비콘은 민감정보 없이 주문 참조 ID 만 광고한다(deviceName). 식별자/rssi 는 로깅하지 않는다.
        val orderId = scanRecord?.deviceName?.takeIf { it.isNotBlank() } ?: return null
        return ProximitySignal(
            orderId = orderId,
            rssi = rssi,
            estimatedArrivalSeconds = estimateArrivalSecondsFromRssi(rssi),
            atMillis = clock.nowMillis(),
        )
    }

    private fun hasScanPermission(): Boolean =
        requiredBluetoothScanPermissions(Build.VERSION.SDK_INT).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun bluetoothLeScannerOrNull(): BluetoothLeScanner? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return null
        val adapter = manager.adapter ?: return null
        if (!adapter.isEnabled) return null
        return runCatching { adapter.bluetoothLeScanner }.getOrNull()
    }

    private companion object {
        const val SignalBufferCapacity = 16
    }
}
