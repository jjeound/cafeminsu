package com.cafeminsu.data.messaging

import com.cafeminsu.di.ApplicationScope
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * 로그인(인증 상태 진입)마다 디바이스 FCM 토큰을 서버에 등록한다.
 * 앱 시작 시 [start]로 1회 구독을 건다(앱 수명 스코프).
 */
@Singleton
class FcmTokenSessionObserver @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val registrar: FcmTokenRegistrar,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun start() {
        appScope.launch {
            sessionRepository.observeAuthState()
                .filterIsInstance<AuthState.Authenticated>()
                .distinctUntilChanged()
                .collect { registrar.register() }
        }
    }
}
