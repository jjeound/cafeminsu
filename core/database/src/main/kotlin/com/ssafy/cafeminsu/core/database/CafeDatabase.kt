package com.ssafy.cafeminsu.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssafy.cafeminsu.core.database.dao.CartDao
import com.ssafy.cafeminsu.core.database.dao.MenuDao
import com.ssafy.cafeminsu.core.database.dao.NotificationDao
import com.ssafy.cafeminsu.core.database.dao.OrderDao
import com.ssafy.cafeminsu.core.database.dao.StampDao
import com.ssafy.cafeminsu.core.database.dao.StoreDao
import com.ssafy.cafeminsu.core.database.model.entity.cart.CartItemEntity
import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.ssafy.cafeminsu.core.database.model.entity.notification.NotificationEntity
import com.ssafy.cafeminsu.core.database.model.entity.order.OrderEntity
import com.ssafy.cafeminsu.core.database.model.entity.stamp.StampEntity
import com.ssafy.cafeminsu.core.database.model.entity.stamp.StampHistoryEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreSearchEntity

@Database(
    entities = [
        CartItemEntity::class,
        StoreEntity::class,
        StoreDetailEntity::class,
        StoreSearchEntity::class,
        MenuEntity::class,
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
    abstract fun cartDao(): CartDao

    abstract fun storeDao(): StoreDao

    abstract fun menuDao(): MenuDao

    abstract fun notificationDao(): NotificationDao

    abstract fun orderDao(): OrderDao

    abstract fun stampDao(): StampDao
}
