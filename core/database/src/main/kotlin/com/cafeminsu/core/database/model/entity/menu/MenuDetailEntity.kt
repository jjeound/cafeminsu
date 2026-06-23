package com.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_details")
data class MenuDetailEntity(
    @PrimaryKey val menuId: Long,
    val description: String,
    val imageUrl: String?,
)
