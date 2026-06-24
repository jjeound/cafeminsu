package com.cafeminsu.data.platform

import android.app.Activity
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 친구 선택/메시지 전송 직전에 카카오 `friends`·`talk_message` 추가 스코프 동의를
 * 증분(loginWithNewScopes)으로 보장한다. Activity/SDK 의존이므로 플랫폼 레이어에만 둔다.
 */
interface KakaoTalkScopeProvider {
    suspend fun ensureFriendMessageScopes(activity: Activity): AppResult<Unit>
}

class RealKakaoTalkScopeProvider @Inject constructor() : KakaoTalkScopeProvider {
    override suspend fun ensureFriendMessageScopes(activity: Activity): AppResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithNewScopes(
                context = activity,
                scopes = FRIEND_MESSAGE_SCOPES,
            ) { token, error ->
                continuation.resumeIfActive(
                    friendMessageScopeConsentResult(
                        tokenPresent = token != null,
                        cancelled = error?.isScopeConsentCancelled() ?: false,
                        failed = error != null,
                    ),
                )
            }
        }

    private fun Throwable.isScopeConsentCancelled(): Boolean =
        this is ClientError && reason == ClientErrorCause.Cancelled

    private fun <T> CancellableContinuation<AppResult<T>>.resumeIfActive(result: AppResult<T>) {
        if (isActive) {
            resume(result)
        }
    }

    private companion object {
        val FRIEND_MESSAGE_SCOPES = listOf("friends", "talk_message")
    }
}

internal fun friendMessageScopeConsentResult(
    tokenPresent: Boolean,
    cancelled: Boolean,
    failed: Boolean,
): AppResult<Unit> =
    when {
        cancelled -> AppResult.Failure(DomainError.Unauthorized)
        failed -> AppResult.Failure(DomainError.Unknown)
        tokenPresent -> AppResult.Success(Unit)
        else -> AppResult.Failure(DomainError.Unknown)
    }
