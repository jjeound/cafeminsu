package com.ssafy.cafeminsu.core.database.di

import com.ssafy.cafeminsu.core.database.CafeDatabase
import com.ssafy.cafeminsu.core.database.dao.CartDao
import com.ssafy.cafeminsu.core.database.dao.MenuDao
import com.ssafy.cafeminsu.core.database.dao.NotificationDao
import com.ssafy.cafeminsu.core.database.dao.OrderDao
import com.ssafy.cafeminsu.core.database.dao.StampDao
import com.ssafy.cafeminsu.core.database.dao.StoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun provideCartDao(
        database: CafeDatabase
    ): CartDao = database.cartDao()

    @Provides
    fun provideStoreDao(
        database: CafeDatabase
    ): StoreDao = database.storeDao()

    @Provides
    fun provideMenuDao(
        database: CafeDatabase
    ): MenuDao = database.menuDao()

    @Provides
    fun provideOrderDao(
        database: CafeDatabase
    ): OrderDao = database.orderDao()

    @Provides
    fun provideStampDao(
        database: CafeDatabase
    ): StampDao = database.stampDao()

    @Provides
    fun provideNotificationDao(
        database: CafeDatabase
    ): NotificationDao = database.notificationDao()
}
