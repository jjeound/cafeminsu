package com.cafeminsu.data.proximity

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실제 BLE 스캔은 에뮬레이터/JVM 에서 검증 불가하므로(실기기 수동 검증), 여기서는 안드로이드 비종속
 * 순수 헬퍼(권한 가드·rssi→도착초 매핑)만 단위로 검증한다.
 */
class AndroidBeaconScannerTest {
    @Test
    fun strongerRssiMeansSoonerArrival() {
        val near = estimateArrivalSecondsFromRssi(rssi = -55)
        val far = estimateArrivalSecondsFromRssi(rssi = -92)

        assertTrue("가까운(강한) 신호가 더 짧은 도착초", near < far)
    }

    @Test
    fun arrivalSecondsAreClampedToConfiguredRange() {
        val veryNear = estimateArrivalSecondsFromRssi(rssi = 0)
        val veryFar = estimateArrivalSecondsFromRssi(rssi = -120)

        assertEquals(MinBeaconArrivalSeconds, veryNear)
        assertEquals(MaxBeaconArrivalSeconds, veryFar)
    }

    @Test
    fun modernSdkRequiresBluetoothScanPermissions() {
        val permissions = requiredBluetoothScanPermissions(sdkInt = Build.VERSION_CODES.S)

        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
            permissions,
        )
    }

    @Test
    fun legacySdkFallsBackToLocationPermission() {
        val permissions = requiredBluetoothScanPermissions(sdkInt = Build.VERSION_CODES.R)

        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), permissions)
    }
}
