package com.cafeminsu.core.database.model.entity.stamp

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "stamp_histories",
    primaryKeys = ["storeId", "createdAt"],
    indices = [Index("storeId")],
)
data class StampHistoryEntity(
    val storeId: Long,
    val earnedCount: Int,
    val createdAt: String,
)
