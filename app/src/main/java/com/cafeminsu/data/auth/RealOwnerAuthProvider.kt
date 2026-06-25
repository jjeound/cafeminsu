package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.MyStoreRes
import com.cafeminsu.data.remote.OwnerLoginReq
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toOwnerLoginExchange
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
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
    // 매장명 실연동: 매장 목록·이름은 stores/my 에서 가져온다(로그인 닉네임 아님).
    private val ownerOrderApi: OwnerOrderApi,
    private val tokenStore: SessionTokenStore,
    private val preferences: UserPreferencesDataStore,
) : OwnerAuthProvider {
    @Volatile
    private var ownerProfile: OwnerProfile? = null

    // selectStore 에서 이름을 찾을 수 있도록 마지막 getStores 결과를 캐시한다.
    @Volatile
    private var cachedStores: List<OwnerStore> = emptyList()

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> {
        val exchange = when (
            val response = runCatchingToAppResult {
                authApi.ownerLogin(OwnerLoginReq(loginId = loginId, password = password))
            }
        ) {
            is AppResult.Success -> response.data.toOwnerLoginExchange(loginId)
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

    override suspend fun getStores(): AppResult<List<OwnerStore>> {
        val current = ownerProfile
            ?: return AppResult.Failure(DomainError.Unauthorized)
        // 매장명·목록은 stores/my 실서버 값을 단일 진실로 쓴다.
        return when (val response = runCatchingToAppResult { ownerOrderApi.getMyStores() }) {
            is AppResult.Success -> {
                val stores = response.data.mapNotNull { it.toOwnerStoreOrNull() }
                if (stores.isEmpty()) {
                    // 서버가 매장을 안 주면(빈 배열) 로그인 매장 단일 항목으로 폴백한다.
                    AppResult.Success(loginStoreFallback(current))
                } else {
                    cachedStores = stores
                    AppResult.Success(stores)
                }
            }
            // 조회 실패 시에도 대시보드가 동작하도록 로그인 매장으로 폴백한다(무회귀).
            is AppResult.Failure -> AppResult.Success(loginStoreFallback(current))
        }
    }

    override suspend fun selectStore(storeId: String): AppResult<OwnerProfile> {
        val current = ownerProfile
            ?: return AppResult.Failure(DomainError.Unauthorized)
        if (storeId == current.storeId) return AppResult.Success(current)
        // 캐시된 stores/my 목록에서 선택 매장을 찾아 활성 프로필을 갱신한다.
        val store = cachedStores.firstOrNull { it.id == storeId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updated = current.copy(storeId = store.id, storeName = store.name)
        ownerProfile = updated
        return AppResult.Success(updated)
    }

    private fun loginStoreFallback(current: OwnerProfile): List<OwnerStore> =
        listOf(OwnerStore(id = current.storeId, name = current.storeName))

    private fun MyStoreRes.toOwnerStoreOrNull(): OwnerStore? {
        val id = id ?: return null
        val storeName = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return OwnerStore(id = id.toString(), name = storeName)
    }
}
