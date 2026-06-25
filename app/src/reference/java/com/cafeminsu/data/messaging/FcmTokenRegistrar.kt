package com.cafeminsu.data.messaging

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.FcmTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디바이스 토큰 획득 → 서버 등록을 조율한다.
 * - 인증되지 않은 상태에서는 등록을 건너뛴다(로그인 시 [FcmTokenSessionObserver]가 재시도).
 * - 토큰 회전(`onNewToken`)은 [explicitToken]으로 전달해 프로바이더 조회를 생략한다.
 */
@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val tokenProvider: DeviceMessagingTokenProvider,
    private val fcmTokenRepository: FcmTokenRepository,
    private val sessionStateHolder: SessionStateHolder,
) {
    suspend fun register(explicitToken: String? = null): AppResult<Unit> {
        if (sessionStateHolder.authState.value !is AuthState.Authenticated) {
            return AppResult.Success(Unit)
        }

        val token = (explicitToken ?: tokenProvider.currentToken())
            ?.takeIf { it.isNotBlank() }
            ?: return AppResult.Failure(DomainError.Unknown)

        return fcmTokenRepository.register(token)
    }
}
