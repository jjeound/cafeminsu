package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cafeminsu.core.database.model.entity.notification.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getNotificationEntities(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    fun getNotificationEntity(notificationId: Long): Flow<NotificationEntity>

    @Insert
    suspend fun insertNotificationEntities(notificationEntities: List<NotificationEntity>)

    @Update
    suspend fun updateNotificationEntities(notificationEntities: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationEntitiesAsRead()

    @Query("DELETE FROM notifications WHERE id IN (:notificationIds)")
    suspend fun deleteNotificationEntities(notificationIds: List<Long>)
}
