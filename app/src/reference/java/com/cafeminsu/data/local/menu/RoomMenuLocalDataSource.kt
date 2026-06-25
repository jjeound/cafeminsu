package com.cafeminsu.data.local.menu

import com.cafeminsu.domain.model.MenuItem
import com.squareup.moshi.Moshi
import javax.inject.Inject

/**
 * 매장별 메뉴 로컬 캐시 접근. 리포지토리는 Room DAO 가 아니라 이 인터페이스에 의존해
 * 단위 테스트에서 가짜로 대체할 수 있게 한다.
 */
interface MenuLocalDataSource {
    suspend fun cachedMenus(storeId: String): List<MenuItem>

    suspend fun replaceMenus(storeId: String, menus: List<MenuItem>)
}

class RoomMenuLocalDataSource @Inject constructor(
    private val menuDao: MenuDao,
    private val moshi: Moshi,
) : MenuLocalDataSource {
    override suspend fun cachedMenus(storeId: String): List<MenuItem> =
        menuDao.byStore(storeId).map { it.toMenuItem(moshi) }

    override suspend fun replaceMenus(storeId: String, menus: List<MenuItem>) {
        // 매장 단위 전체 교체: 사라진 메뉴가 캐시에 남지 않도록 해당 매장만 비운 뒤 다시 채운다.
        menuDao.clearStore(storeId)
        menuDao.upsertAll(menus.map { it.toMenuEntity(storeId, moshi) })
    }
}
