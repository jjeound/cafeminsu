package com.cafeminsu.di

import android.content.Context
import com.cafeminsu.BuildConfig
import com.cafeminsu.data.auth.MockLoginProvider
import com.cafeminsu.data.platform.RealKakaoLoginProvider
import com.cafeminsu.domain.auth.LoginProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideLoginProvider(
        @ApplicationContext context: Context,
    ): LoginProvider =
        selectLoginProvider(
            kakaoNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
            realFactory = { RealKakaoLoginProvider(context) },
            mockFactory = { MockLoginProvider() },
        )
}

internal fun selectLoginProvider(
    kakaoNativeAppKey: String,
    realFactory: () -> LoginProvider,
    mockFactory: () -> LoginProvider,
): LoginProvider =
    if (kakaoNativeAppKey.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }
