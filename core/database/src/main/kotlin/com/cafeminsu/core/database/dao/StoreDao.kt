package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.cafeminsu.core.database.model.entity.store.StoreEntity
import com.cafeminsu.core.database.model.entity.store.StoreSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query(
        """
        SELECT stores.* FROM stores
        INNER JOIN store_search_results ON stores.id = store_search_results.storeId
        WHERE store_search_results.query = :query AND store_search_results.page = :page
        ORDER BY store_search_results.position
        """,
    )
    fun getStoreEntities(query: String, page: Int): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :storeId")
    fun getStoreEntity(storeId: Long): Flow<StoreEntity>

    @Query("SELECT * FROM store_details WHERE storeId = :storeId")
    fun getStoreDetailEntity(storeId: Long): Flow<StoreDetailEntity>

    @Insert
    suspend fun insertStoreEntities(storeEntities: List<StoreEntity>)

    @Update
    suspend fun updateStoreEntities(storeEntities: List<StoreEntity>)

    @Insert
    suspend fun insertStoreDetailEntities(storeDetailEntities: List<StoreDetailEntity>)

    @Update
    suspend fun updateStoreDetailEntities(storeDetailEntities: List<StoreDetailEntity>)

    @Insert
    suspend fun insertStoreSearchEntities(storeSearchEntities: List<StoreSearchEntity>)

    @Update
    suspend fun updateStoreSearchEntities(storeSearchEntities: List<StoreSearchEntity>)

    @Query("DELETE FROM store_search_results WHERE query = :query AND page = :page")
    suspend fun deleteStoreSearchEntities(query: String, page: Int)

    @Query("DELETE FROM stores WHERE id IN (:storeIds)")
    suspend fun deleteStoreEntities(storeIds: List<Long>)
}
