package com.cafeminsu.di

import com.cafeminsu.data.location.AndroidLocationProvider
import com.cafeminsu.domain.location.LocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 위치 제공 DI. 도메인 [LocationProvider] 를 프레임워크 [AndroidLocationProvider] 로 바인딩한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindLocationProvider(provider: AndroidLocationProvider): LocationProvider
}
