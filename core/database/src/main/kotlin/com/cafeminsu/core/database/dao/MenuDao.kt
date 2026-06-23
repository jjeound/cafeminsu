package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cafeminsu.core.database.model.entity.menu.MenuDetailEntity
import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE storeId = :storeId AND (:category IS NULL OR category = :category)")
    fun getMenuEntities(storeId: Long, category: String?): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE id = :menuId")
    fun getMenuEntity(menuId: Long): Flow<MenuEntity>

    @Query("SELECT * FROM menu_details WHERE menuId = :menuId")
    fun getMenuDetailEntity(menuId: Long): Flow<MenuDetailEntity>

    @Query("SELECT * FROM menu_options WHERE menuId = :menuId")
    fun getMenuOptionEntities(menuId: Long): Flow<List<MenuOptionEntity>>

    @Insert
    suspend fun insertMenuEntities(menuEntities: List<MenuEntity>)

    @Update
    suspend fun updateMenuEntities(menuEntities: List<MenuEntity>)

    @Insert
    suspend fun insertMenuDetailEntities(menuDetailEntities: List<MenuDetailEntity>)

    @Update
    suspend fun updateMenuDetailEntities(menuDetailEntities: List<MenuDetailEntity>)

    @Insert
    suspend fun insertMenuOptionEntities(menuOptionEntities: List<MenuOptionEntity>)

    @Update
    suspend fun updateMenuOptionEntities(menuOptionEntities: List<MenuOptionEntity>)

    @Query("DELETE FROM menus WHERE id IN (:menuIds)")
    suspend fun deleteMenuEntities(menuIds: List<Long>)

    @Query("DELETE FROM menu_options WHERE menuId = :menuId")
    suspend fun deleteMenuOptionEntities(menuId: Long)
}
