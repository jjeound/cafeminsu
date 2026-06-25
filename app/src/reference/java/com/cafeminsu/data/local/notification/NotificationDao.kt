package com.cafeminsu.data.local.notification

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NotificationDao {
    @Upsert
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("DELETE FROM notifications")
    suspend fun clear()
}
