package com.cafeminsu.ui.navigation

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserRole
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first

/**
 * 인증 상태 → 시작 화면 라우트 매핑(순수). 점주는 점주 홈, 고객은 홈, 그 외(게스트/만료/미상)는 로그인.
 * 기존 인앱 SplashGate 의 라우팅 규칙을 이식한 것.
 */
internal fun resolveStartDestination(authState: AuthState): String =
    when (authState) {
        is AuthState.Authenticated ->
            if (authState.role == UserRole.Owner) Routes.OWNER_HOME else Routes.HOME

        AuthState.Guest,
        AuthState.Expired,
        AuthState.Unknown,
        -> Routes.LOGIN
    }

/**
 * 콜드 스타트 초기 인증을 해석해 시작 라우트를 정한다(시스템 스플래시가 떠 있는 동안 호출).
 * 첫 인증 상태가 미상이면 토큰 갱신을 1회 시도하고, 실패 시 게스트로 간주한다.
 */
internal suspend fun resolveInitialStartDestination(
    sessionRepository: SessionRepository,
): String {
    val current = sessionRepository.observeAuthState().first()
    val resolved = if (current == AuthState.Unknown) {
        when (val result = sessionRepository.refreshOnce()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> AuthState.Guest
        }
    } else {
        current
    }
    return resolveStartDestination(resolved)
}
