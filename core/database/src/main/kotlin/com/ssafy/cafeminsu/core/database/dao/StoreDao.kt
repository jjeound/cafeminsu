package com.ssafy.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreSearchEntity
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
    fun getStoreEntity(storeId: Long): Flow<StoreEntity?>

    @Query("SELECT * FROM store_details WHERE storeId = :storeId")
    fun getStoreDetailEntity(storeId: Long): Flow<StoreDetailEntity?>

    @Upsert
    suspend fun upsertStoreEntities(storeEntities: List<StoreEntity>)

    @Upsert
    suspend fun upsertStoreDetailEntity(storeDetailEntity: StoreDetailEntity)

    @Upsert
    suspend fun upsertStoreSearchEntities(storeSearchEntities: List<StoreSearchEntity>)

    @Query("DELETE FROM store_search_results WHERE query = :query AND page = :page")
    suspend fun deleteStoreSearchEntities(query: String, page: Int)

    @Query("DELETE FROM stores WHERE id IN (:storeIds)")
    suspend fun deleteStoreEntities(storeIds: List<Long>)

    @Transaction
    suspend fun replaceStoreSearchResults(
        query: String,
        page: Int,
        storeEntities: List<StoreEntity>,
        storeSearchEntities: List<StoreSearchEntity>,
    ) {
        deleteStoreSearchEntities(query, page)
        upsertStoreEntities(storeEntities)
        upsertStoreSearchEntities(storeSearchEntities)
    }
}
