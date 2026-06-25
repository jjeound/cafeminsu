package com.ssafy.cafeminsu.core.database.model.entity.order

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [Index("createdAtMillis")],
)
data class OrderEntity(
    @PrimaryKey val id: Long,
    val orderNumber: String,
    val totalAmount: Int,
    val status: String,
    val createdAtMillis: Long,
)
