package com.cafeminsu.di

import android.content.Context
import androidx.room.Room
import com.cafeminsu.data.local.db.CafeDatabase
import com.cafeminsu.data.local.menu.MenuDao
import com.cafeminsu.data.local.menu.MenuLocalDataSource
import com.cafeminsu.data.local.menu.RoomMenuLocalDataSource
import com.cafeminsu.data.local.store.RoomStoreLocalDataSource
import com.cafeminsu.data.local.store.StoreDao
import com.cafeminsu.data.local.store.StoreLocalDataSource
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCafeDatabase(
        @ApplicationContext context: Context,
    ): CafeDatabase =
        Room.databaseBuilder(context, CafeDatabase::class.java, CacheDatabaseName)
            // 캐시 전용 DB라 스키마가 바뀌면 재생성으로 충분하다(원본은 서버) → 파괴적 마이그레이션 허용.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideStoreDao(database: CafeDatabase): StoreDao = database.storeDao()

    @Provides
    @Singleton
    fun provideStoreLocalDataSource(storeDao: StoreDao): StoreLocalDataSource =
        RoomStoreLocalDataSource(storeDao)

    @Provides
    @Singleton
    fun provideMenuDao(database: CafeDatabase): MenuDao = database.menuDao()

    @Provides
    @Singleton
    fun provideMenuLocalDataSource(menuDao: MenuDao, moshi: Moshi): MenuLocalDataSource =
        RoomMenuLocalDataSource(menuDao, moshi)
}

private const val CacheDatabaseName = "cafeminsu-cache.db"
