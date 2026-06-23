package com.cafeminsu.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cafeminsu.data.local.menu.MenuDao
import com.cafeminsu.data.local.menu.MenuEntity
import com.cafeminsu.data.local.notification.NotificationDao
import com.cafeminsu.data.local.notification.NotificationEntity
import com.cafeminsu.data.local.order.OrderEntity
import com.cafeminsu.data.local.order.OrderHistoryDao
import com.cafeminsu.data.local.store.StoreDao
import com.cafeminsu.data.local.store.StoreEntity

/**
 * 로컬 캐시 DB. 도메인 원본은 서버이므로 여기에는 읽기 캐시 엔티티만 둔다.
 * 토큰·세션은 절대 담지 않는다(EncryptedDataStore 전용, SECURITY.md).
 * 스키마 변경 시 파괴적 마이그레이션(DatabaseModule) 으로 재생성한다 — 캐시라 데이터 손실이 안전하다.
 */
@Database(
    entities = [
        StoreEntity::class,
        MenuEntity::class,
        NotificationEntity::class,
        OrderEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class CafeDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    abstract fun menuDao(): MenuDao

    abstract fun notificationDao(): NotificationDao

    abstract fun orderHistoryDao(): OrderHistoryDao
}
