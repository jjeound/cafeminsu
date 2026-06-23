package com.cafeminsu.core.database.model.entity.store

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_details")
data class StoreDetailEntity(
    @PrimaryKey val storeId: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val businessHours: String,
    val imageUrl: String?,
)
