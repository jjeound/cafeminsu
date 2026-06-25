package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore

interface OwnerAuthProvider {
    suspend fun login(loginId: String, password: String): AppResult<OwnerProfile>
    suspend fun logout(): AppResult<Unit>
    suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile>

    /** 로그인한 점주가 운영하는 매장 목록. 실서버 다중매장 API 부재 시 단일 매장만 반환한다. */
    suspend fun getStores(): AppResult<List<OwnerStore>>

    /** 활성 매장을 전환하고, 전환된 매장 기준 [OwnerProfile] 을 반환한다. */
    suspend fun selectStore(storeId: String): AppResult<OwnerProfile>
}
