package com.ssafy.cafeminsu.core.database.model.entity.stamp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "stamp_histories",
    primaryKeys = ["storeId", "createdAtMillis"],
    foreignKeys = [
        ForeignKey(
            entity = StampEntity::class,
            parentColumns = ["storeId"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("storeId")],
)
data class StampHistoryEntity(
    val storeId: Long,
    val earnedCount: Int,
    val createdAtMillis: Long,
)
