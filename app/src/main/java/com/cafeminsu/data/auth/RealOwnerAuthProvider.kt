package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.OwnerLoginReq
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toOwnerLoginExchange
import com.cafeminsu.data.remote.unwrap
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 점주 아이디/비밀번호 로그인을 실서버(`POST api/user/owner-login`)에 연동한다.
 * 발급받은 JWT 는 [SessionTokenStore] 에 저장해 이후 점주 인증 호출에 재사용한다.
 * 매장 영업 토글은 대응 서버 엔드포인트가 없어 로컬에서만 반영하되, [UserPreferencesDataStore] 에
 * 영속해 앱 재시작·재로그인 시 마지막 토글 상태를 복원한다.
 */
@Singleton
class RealOwnerAuthProvider @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: SessionTokenStore,
    private val preferences: UserPreferencesDataStore,
) : OwnerAuthProvider {
    @Volatile
    private var ownerProfile: OwnerProfile? = null

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> {
        val exchange = when (
            val response = runCatchingToAppResult {
                authApi.ownerLogin(OwnerLoginReq(loginId = loginId, password = password))
            }
        ) {
            is AppResult.Success -> response.data.unwrap { it.toOwnerLoginExchange(loginId) }
            is AppResult.Failure -> response
        }

        return when (exchange) {
            is AppResult.Success -> {
                tokenStore.save(exchange.data.tokens)
                // 이전에 명시적으로 저장한 영업 토글이 있으면 복원, 없으면 매핑 기본값 유지.
                val persisted = preferences.readOwnerStoreOpenOrNull()
                val profile = exchange.data.ownerProfile.copy(
                    isStoreOpen = persisted ?: exchange.data.ownerProfile.isStoreOpen,
                )
                ownerProfile = profile
                AppResult.Success(profile)
            }

            is AppResult.Failure -> exchange
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        tokenStore.clear()
        ownerProfile = null
        return AppResult.Success(Unit)
    }

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> {
        val current = ownerProfile
            ?: return AppResult.Failure(DomainError.Unauthorized)
        val updated = current.copy(isStoreOpen = open)
        ownerProfile = updated
        preferences.setOwnerStoreOpen(open)
        return AppResult.Success(updated)
    }
}
