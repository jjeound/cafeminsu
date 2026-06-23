package com.cafeminsu.messaging

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cafeminsu.R
import com.cafeminsu.data.messaging.FcmTokenRegistrar
import com.cafeminsu.data.messaging.PushNotificationContent
import com.cafeminsu.data.messaging.PushNotificationFactory
import com.cafeminsu.di.ApplicationScope
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * FCM 수신 진입점.
 * - [onNewToken]: 토큰 회전 시 서버에 재등록(인증된 경우에만 — [FcmTokenRegistrar]가 게이팅).
 * - [onMessageReceived]: 포그라운드/데이터 메시지를 로컬 알림으로 표시.
 */
@AndroidEntryPoint
class CafeMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var registrar: FcmTokenRegistrar

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        appScope.launch { registrar.register(explicitToken = token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val content = PushNotificationFactory.from(
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body,
            data = message.data,
        ) ?: return
        showNotification(content)
    }

    private fun showNotification(content: PushNotificationContent) {
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                content.channelId,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            ).setName(content.channelName).build(),
        )

        if (!hasPostNotificationsPermission()) {
            return // 권한 미허용 — 조용히 무시(크래시 금지)
        }

        val notification = NotificationCompat.Builder(this, content.channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            manager.notify(content.notificationId, notification)
        } catch (_: SecurityException) {
            // 일부 OEM/권한 변경 레이스 — 무시
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
