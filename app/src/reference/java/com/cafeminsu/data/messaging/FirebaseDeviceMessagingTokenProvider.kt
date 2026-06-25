package com.cafeminsu.data.messaging

import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Firebase 메시징 토큰 제공자.
 * Firebase 미초기화·네트워크 실패 등 어떤 오류에도 예외를 던지지 않고 null 로 폴백한다(크래시 금지).
 */
@Singleton
class FirebaseDeviceMessagingTokenProvider @Inject constructor() : DeviceMessagingTokenProvider {
    override suspend fun currentToken(): String? =
        runCatching {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (continuation.isActive) continuation.resume(token)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        }.getOrNull()
}
