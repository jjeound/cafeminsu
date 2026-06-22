package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
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
 * 매장 영업 토글은 대응 서버 엔드포인트가 없어 로그인된 프로필 위에서 로컬로만 반영한다.
 */
@Singleton
class RealOwnerAuthProvider @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: SessionTokenStore,
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
                ownerProfile = exchange.data.ownerProfile
                AppResult.Success(exchange.data.ownerProfile)
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
        return AppResult.Success(updated)
    }
}
