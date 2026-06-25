package com.ssafy.cafeminsu.core.data.di

import com.ssafy.cafeminsu.core.data.kakao.DefaultKakaoAuthDataSource
import com.ssafy.cafeminsu.core.data.kakao.KakaoAuthDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class KakaoModule {

    @Binds
    internal abstract fun bindKakaoAuthDataSource(
        kakaoAuthDataSource: DefaultKakaoAuthDataSource,
    ): KakaoAuthDataSource
}
