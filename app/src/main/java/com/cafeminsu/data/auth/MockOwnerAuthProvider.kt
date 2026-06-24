package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 점주 Mock 인증. 실서버 다중매장 API 가 없으므로 데모 점주는 여러 매장(강남/홍대/판교)을 보유한 것으로
 * 시드하고, 헤더 매장 선택 UX 를 검증할 수 있게 한다. 영업 토글은 매장별로 보관한다.
 */
@Singleton
class MockOwnerAuthProvider @Inject constructor() : OwnerAuthProvider {
    private val stores = mutableListOf(
        MockStore(id = "store-gangnam", name = "강남점", isStoreOpen = true),
        MockStore(id = "store-hongdae", name = "홍대점", isStoreOpen = true),
        MockStore(id = "store-pangyo", name = "판교점", isStoreOpen = false),
    )
    private var loginId = DefaultLoginId
    private var selectedStoreId = stores.first().id

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> {
        this.loginId = loginId
        selectedStoreId = stores.first().id
        return AppResult.Success(currentProfile())
    }

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> {
        val store = stores.first { it.id == selectedStoreId }
        store.isStoreOpen = open
        return AppResult.Success(currentProfile())
    }

    override suspend fun getStores(): AppResult<List<OwnerStore>> =
        AppResult.Success(stores.map { OwnerStore(id = it.id, name = it.name) })

    override suspend fun selectStore(storeId: String): AppResult<OwnerProfile> {
        if (stores.none { it.id == storeId }) return AppResult.Failure(DomainError.NotFound)
        selectedStoreId = storeId
        return AppResult.Success(currentProfile())
    }

    private fun currentProfile(): OwnerProfile {
        val store = stores.first { it.id == selectedStoreId }
        return OwnerProfile(
            id = "owner-demo",
            storeId = store.id,
            storeName = store.name,
            loginId = loginId,
            isStoreOpen = store.isStoreOpen,
        )
    }

    private class MockStore(
        val id: String,
        val name: String,
        var isStoreOpen: Boolean,
    )

    private companion object {
        const val DefaultLoginId = "owner"
    }
}
