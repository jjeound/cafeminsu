package com.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_options",
    indices = [Index("menuId")],
)
data class MenuOptionEntity(
    @PrimaryKey val id: Long,
    val menuId: Long,
    val groupName: String,
    val name: String,
    val additionalPrice: Int,
    val isDefault: Boolean,
)
