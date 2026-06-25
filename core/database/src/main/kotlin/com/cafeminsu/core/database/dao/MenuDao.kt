package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.cafeminsu.core.database.model.relation.menu.MenuWithOptions
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Query(
        """
        SELECT *
        FROM menus
        WHERE storeId = :storeId
        AND (:category IS NULL OR category = :category)
        ORDER BY id ASC
        """
    )
    fun getMenuEntities(
        storeId: Long,
        category: String?,
    ): Flow<List<MenuEntity>>

    @Transaction
    @Query("SELECT * FROM menus WHERE id = :menuId")
    fun getMenuWithOptions(
        menuId: Long,
    ): Flow<MenuWithOptions?>

    @Query("SELECT * FROM menus WHERE id = :menuId")
    suspend fun getMenuEntity(
        menuId: Long,
    ): MenuEntity?

    @Upsert
    suspend fun upsertMenuEntities(
        menuEntities: List<MenuEntity>,
    )

    @Upsert
    suspend fun upsertMenuEntity(
        menuEntity: MenuEntity,
    )

    @Upsert
    suspend fun upsertMenuOptionEntities(
        optionEntities: List<MenuOptionEntity>,
    )

    @Query("DELETE FROM menus WHERE storeId = :storeId")
    suspend fun deleteMenusByStoreId(
        storeId: Long,
    )

    @Query("DELETE FROM menu_options WHERE menuId = :menuId")
    suspend fun deleteMenuOptionsByMenuId(
        menuId: Long,
    )

    @Transaction
    suspend fun replaceMenus(
        storeId: Long,
        menuEntities: List<MenuEntity>,
    ) {
        deleteMenusByStoreId(storeId)
        upsertMenuEntities(menuEntities)
    }

    @Transaction
    suspend fun replaceMenuWithOptions(
        menuEntity: MenuEntity,
        optionEntities: List<MenuOptionEntity>,
    ) {
        upsertMenuEntity(menuEntity)
        deleteMenuOptionsByMenuId(menuEntity.id)
        upsertMenuOptionEntities(optionEntities)
    }
}
