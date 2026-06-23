package com.cafeminsu

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.cafeminsu.data.messaging.FcmTokenSessionObserver
import com.cafeminsu.data.messaging.PushNotificationFactory
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CafeApplication : Application() {
    @Inject
    lateinit var fcmTokenSessionObserver: FcmTokenSessionObserver

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }

        createDefaultNotificationChannel()
        fcmTokenSessionObserver.start()
    }

    private fun createDefaultNotificationChannel() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(
                PushNotificationFactory.CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            ).setName(PushNotificationFactory.CHANNEL_NAME).build(),
        )
    }
}
