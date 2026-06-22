package com.cafeminsu.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cafeminsu.data.local.menu.MenuDao
import com.cafeminsu.data.local.menu.MenuEntity
import com.cafeminsu.data.local.store.StoreDao
import com.cafeminsu.data.local.store.StoreEntity

/**
 * 로컬 캐시 DB. 도메인 원본은 서버이므로 여기에는 읽기 캐시 엔티티만 둔다.
 * 이후 step 에서 주문 내역 등 엔티티와 version 을 늘린다.
 * 스키마 변경 시 파괴적 마이그레이션(DatabaseModule) 으로 재생성한다 — 캐시라 데이터 손실이 안전하다.
 */
@Database(
    entities = [StoreEntity::class, MenuEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CafeDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    abstract fun menuDao(): MenuDao
}
