package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cafeminsu.core.database.model.entity.stamp.StampEntity
import com.cafeminsu.core.database.model.entity.stamp.StampHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps")
    fun getStampEntities(): Flow<List<StampEntity>>

    @Query("SELECT * FROM stamps WHERE storeId = :storeId")
    fun getStampEntity(storeId: Long): Flow<StampEntity>

    @Query("SELECT * FROM stamp_histories WHERE storeId = :storeId ORDER BY createdAt DESC")
    fun getStampHistoryEntities(storeId: Long): Flow<List<StampHistoryEntity>>

    @Insert
    suspend fun insertStampEntities(stampEntities: List<StampEntity>)

    @Update
    suspend fun updateStampEntities(stampEntities: List<StampEntity>)

    @Insert
    suspend fun insertStampHistoryEntities(stampHistoryEntities: List<StampHistoryEntity>)

    @Update
    suspend fun updateStampHistoryEntities(stampHistoryEntities: List<StampHistoryEntity>)

    @Query("DELETE FROM stamps WHERE storeId IN (:storeIds)")
    suspend fun deleteStampEntities(storeIds: List<Long>)

    @Query("DELETE FROM stamp_histories WHERE storeId = :storeId")
    suspend fun deleteStampHistoryEntities(storeId: Long)
}
