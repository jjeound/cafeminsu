package com.cafeminsu.data.local.menu

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MenuDao {
    @Upsert
    suspend fun upsertAll(menus: List<MenuEntity>)

    @Query("SELECT * FROM menus WHERE storeId = :storeId")
    suspend fun byStore(storeId: String): List<MenuEntity>

    @Query("DELETE FROM menus WHERE storeId = :storeId")
    suspend fun clearStore(storeId: String)
}
