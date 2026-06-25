package com.cafeminsu.data.local.store

import com.cafeminsu.domain.model.Store
import javax.inject.Inject

/**
 * 매장 목록 로컬 캐시 접근. 리포지토리는 Room DAO 가 아니라 이 인터페이스에 의존해
 * 단위 테스트에서 가짜로 대체할 수 있게 한다.
 */
interface StoreLocalDataSource {
    suspend fun cachedStores(): List<Store>

    suspend fun replaceStores(stores: List<Store>)
}

class RoomStoreLocalDataSource @Inject constructor(
    private val storeDao: StoreDao,
) : StoreLocalDataSource {
    override suspend fun cachedStores(): List<Store> = storeDao.getAll().toStores()

    override suspend fun replaceStores(stores: List<Store>) {
        // 목록 전체 교체: 사라진 매장이 캐시에 남지 않도록 비운 뒤 다시 채운다.
        storeDao.clear()
        storeDao.upsertAll(stores.toStoreEntities())
    }
}
