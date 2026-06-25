package com.cafeminsu.data.platform

import android.content.Context
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.KakaoOAuthToken
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RealKakaoLoginProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LoginProvider {
    override suspend fun login(): AppResult<AuthState> =
        when (val tokenResult = requestKakaoAccessToken()) {
            is AppResult.Success -> requestUserProfile()
            is AppResult.Failure -> tokenResult
        }

    override suspend fun loginForServerExchange(): AppResult<KakaoOAuthToken> =
        requestKakaoAccessToken()

    override suspend fun logout(): AppResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.logout { error ->
                val result = if (error == null) {
                    AppResult.Success(Unit)
                } else {
                    AppResult.Failure(error.toDomainError())
                }
                continuation.resumeIfActive(result)
            }
        }

    private suspend fun requestKakaoAccessToken(): AppResult<KakaoOAuthToken> =
        suspendCancellableCoroutine { continuation ->
            val accountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                when {
                    error != null -> continuation.resumeIfActive(AppResult.Failure(error.toDomainError()))
                    token != null -> continuation.resumeIfActive(AppResult.Success(token.toKakaoOAuthToken()))
                    else -> continuation.resumeIfActive(AppResult.Failure(DomainError.Unknown))
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    when {
                        error != null && error.isKakaoLoginCancelled() ->
                            continuation.resumeIfActive(AppResult.Failure(DomainError.Unauthorized))

                        error != null ->
                            UserApiClient.instance.loginWithKakaoAccount(
                                context = context,
                                callback = accountCallback,
                            )

                        token != null -> continuation.resumeIfActive(AppResult.Success(token.toKakaoOAuthToken()))
                        else -> continuation.resumeIfActive(AppResult.Failure(DomainError.Unknown))
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(
                    context = context,
                    callback = accountCallback,
                )
            }
        }

    private suspend fun requestUserProfile(): AppResult<AuthState> =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.me { user, error ->
                val result = when {
                    error != null -> AppResult.Failure(error.toDomainError())
                    user != null -> AppResult.Success(user.toAuthenticatedState())
                    else -> AppResult.Failure(DomainError.Unknown)
                }
                continuation.resumeIfActive(result)
            }
        }

    private fun OAuthToken.toKakaoOAuthToken(): KakaoOAuthToken =
        KakaoOAuthToken(accessToken)

    private fun User.toAuthenticatedState(): AuthState =
        kakaoProfileToAuthenticatedState(
            kakaoId = id?.toString(),
            nickname = kakaoAccount?.profile?.nickname,
        )

    private fun Throwable.isKakaoLoginCancelled(): Boolean =
        this is ClientError && reason == ClientErrorCause.Cancelled

    private fun Throwable.toDomainError(): DomainError =
        when {
            isKakaoLoginCancelled() -> DomainError.Unauthorized
            else -> DomainError.Unknown
        }

    private fun <T> CancellableContinuation<AppResult<T>>.resumeIfActive(result: AppResult<T>) {
        if (isActive) {
            resume(result)
        }
    }
}

internal fun kakaoProfileToAuthenticatedState(
    kakaoId: String?,
    nickname: String?,
): AuthState =
    AuthState.Authenticated(
        UserProfile(
            id = kakaoId
                ?.takeIf { it.isNotBlank() }
                ?.let { "kakao-$it" }
                ?: "kakao-user",
            displayName = nickname
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "카카오 사용자",
            phoneLast4 = null,
        ),
    )
