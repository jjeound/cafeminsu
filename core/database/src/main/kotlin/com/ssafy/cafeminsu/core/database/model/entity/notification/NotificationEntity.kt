package com.ssafy.cafeminsu.core.database.model.entity.notification

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [Index("createdAtMillis")],
)
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val relatedEntityId: Long?,
    val createdAtMillis: Long,
)
