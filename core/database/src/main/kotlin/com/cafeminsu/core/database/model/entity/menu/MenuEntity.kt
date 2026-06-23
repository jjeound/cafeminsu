package com.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menus",
    indices = [Index("storeId"), Index("category")],
)
data class MenuEntity(
    @PrimaryKey val id: Long,
    val storeId: Long,
    val name: String,
    val price: Int,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
)
