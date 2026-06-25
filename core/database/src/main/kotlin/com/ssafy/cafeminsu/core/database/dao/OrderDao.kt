package com.ssafy.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ssafy.cafeminsu.core.database.model.entity.order.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun getOrderHistoryEntities(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC LIMIT :limit")
    fun getRecentOrderEntities(limit: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderEntity(orderId: Long): Flow<OrderEntity>

    @Insert
    suspend fun insertOrderEntities(orderEntities: List<OrderEntity>)

    @Update
    suspend fun updateOrderEntities(orderEntities: List<OrderEntity>)

    @Query("DELETE FROM orders WHERE id IN (:orderIds)")
    suspend fun deleteOrderEntities(orderIds: List<Long>)
}
