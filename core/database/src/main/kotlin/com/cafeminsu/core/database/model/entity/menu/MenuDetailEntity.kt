package com.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_details")
data class MenuDetailEntity(
    @PrimaryKey val menuId: Long,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
)
