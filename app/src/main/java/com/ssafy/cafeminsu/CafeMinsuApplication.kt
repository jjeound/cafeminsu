package com.ssafy.cafeminsu

import android.app.Application
import com.cafeminsu.BuildConfig
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CafeMinsuApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}
