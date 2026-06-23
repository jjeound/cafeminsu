package com.cafeminsu.data.local.order

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OrderHistoryDao {
    @Upsert
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("SELECT * FROM order_history ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<OrderEntity>

    @Query("DELETE FROM order_history")
    suspend fun clear()
}
