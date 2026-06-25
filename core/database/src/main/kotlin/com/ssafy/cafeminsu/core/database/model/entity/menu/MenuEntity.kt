package com.ssafy.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey
    val id: Long,
    val storeId: Long,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val imageUrl: String,
    val isAvailable: Boolean,
)
