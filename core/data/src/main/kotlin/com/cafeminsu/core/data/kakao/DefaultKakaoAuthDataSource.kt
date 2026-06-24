package com.cafeminsu.core.data.kakao

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DefaultKakaoAuthDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : KakaoAuthDataSource {
    override suspend fun getAccessToken(): String = requestAccessToken().accessToken

    override suspend fun logout() {
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.logout { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(error)
                }
            }
        }
    }

    private suspend fun requestAccessToken(): OAuthToken = suspendCancellableCoroutine { continuation ->
        val accountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> continuation.resumeWithException(error)
                token != null -> continuation.resume(token)
                else -> continuation.resumeWithException(IllegalStateException("Kakao access token is unavailable"))
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                when {
                    error is ClientError && error.reason == ClientErrorCause.Cancelled ->
                        continuation.resumeWithException(error)

                    error != null -> UserApiClient.instance.loginWithKakaoAccount(context, callback = accountCallback)
                    token != null -> continuation.resume(token)
                    else -> continuation.resumeWithException(IllegalStateException("Kakao access token is unavailable"))
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = accountCallback)
        }
    }
}
