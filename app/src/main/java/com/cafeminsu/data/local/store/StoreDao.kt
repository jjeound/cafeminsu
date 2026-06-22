package com.cafeminsu.data.local.store

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StoreDao {
    @Upsert
    suspend fun upsertAll(stores: List<StoreEntity>)

    @Query("SELECT * FROM stores")
    suspend fun getAll(): List<StoreEntity>

    @Query("DELETE FROM stores")
    suspend fun clear()
}
