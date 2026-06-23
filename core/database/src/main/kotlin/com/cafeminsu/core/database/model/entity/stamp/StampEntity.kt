package com.cafeminsu.core.database.model.entity.stamp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stamps")
data class StampEntity(
    @PrimaryKey val storeId: Long,
    val storeName: String,
    val count: Int,
)
