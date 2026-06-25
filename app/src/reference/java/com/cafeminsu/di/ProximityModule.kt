package com.cafeminsu.di

import com.cafeminsu.data.proximity.SimulatedProximityScanner
import com.cafeminsu.domain.proximity.ProximityScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 근접(BLE) 스캐너 DI. 기본 바인딩은 에뮬레이터/CI 안전한 [SimulatedProximityScanner] 다.
 *
 * 실 BLE 로 교체하려면 아래 바인딩의 파라미터 타입을
 * [com.cafeminsu.data.proximity.AndroidBeaconScanner] 로 바꾼다(권한·블루투스 가용 단말에서만 동작).
 * 실 스캐너도 권한/하드웨어 부재 시 [com.cafeminsu.core.AppResult.Failure] 로 안전 폴백하므로 크래시는 없다.
 *
 * [com.cafeminsu.domain.proximity.ProximitySignalRepository] 는 `@Inject` + `@Singleton` 으로 자동 제공된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProximityModule {
    @Binds
    @Singleton
    abstract fun bindProximityScanner(scanner: SimulatedProximityScanner): ProximityScanner
}
