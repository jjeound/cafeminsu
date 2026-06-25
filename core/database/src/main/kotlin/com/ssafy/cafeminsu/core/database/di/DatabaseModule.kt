package com.ssafy.cafeminsu.core.database.di

import android.content.Context
import androidx.room.Room
import com.ssafy.cafeminsu.core.database.CafeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideCafeDatabase(
        @ApplicationContext context: Context
    ): CafeDatabase = Room.databaseBuilder(
        context,
        CafeDatabase::class.java,
        "cafeminsu.db"
    ).build()
}
