package com.cafeminsu.core.database.model.entity.store

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val address: String,
    val imageUrl: String?,
)
