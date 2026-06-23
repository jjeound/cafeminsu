package com.cafeminsu.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cafeminsu.core.database.dao.MenuDao
import com.cafeminsu.core.database.dao.NotificationDao
import com.cafeminsu.core.database.dao.OrderDao
import com.cafeminsu.core.database.dao.StampDao
import com.cafeminsu.core.database.dao.StoreDao
import com.cafeminsu.core.database.model.entity.menu.MenuDetailEntity
import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.cafeminsu.core.database.model.entity.notification.NotificationEntity
import com.cafeminsu.core.database.model.entity.order.OrderEntity
import com.cafeminsu.core.database.model.entity.stamp.StampEntity
import com.cafeminsu.core.database.model.entity.stamp.StampHistoryEntity
import com.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.cafeminsu.core.database.model.entity.store.StoreEntity
import com.cafeminsu.core.database.model.entity.store.StoreSearchEntity

@Database(
    entities = [
        StoreEntity::class,
        StoreDetailEntity::class,
        StoreSearchEntity::class,
        MenuEntity::class,
        MenuDetailEntity::class,
        MenuOptionEntity::class,
        NotificationEntity::class,
        OrderEntity::class,
        StampEntity::class,
        StampHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CafeDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    abstract fun menuDao(): MenuDao

    abstract fun notificationDao(): NotificationDao

    abstract fun orderDao(): OrderDao

    abstract fun stampDao(): StampDao
}
