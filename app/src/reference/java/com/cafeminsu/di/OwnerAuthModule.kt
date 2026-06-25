package com.cafeminsu.di

import com.cafeminsu.BuildConfig
import com.cafeminsu.data.auth.MockOwnerAuthProvider
import com.cafeminsu.data.auth.RealOwnerAuthProvider
import com.cafeminsu.domain.auth.OwnerAuthProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OwnerAuthModule {
    @Provides
    @Singleton
    fun provideOwnerAuthProvider(
        realProvider: Provider<RealOwnerAuthProvider>,
        mockProvider: Provider<MockOwnerAuthProvider>,
    ): OwnerAuthProvider =
        selectOwnerAuthProvider(
            baseUrl = BuildConfig.BASE_URL,
            realFactory = { realProvider.get() },
            mockFactory = { mockProvider.get() },
        )
}

internal fun selectOwnerAuthProvider(
    baseUrl: String,
    realFactory: () -> OwnerAuthProvider,
    mockFactory: () -> OwnerAuthProvider,
): OwnerAuthProvider =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }
