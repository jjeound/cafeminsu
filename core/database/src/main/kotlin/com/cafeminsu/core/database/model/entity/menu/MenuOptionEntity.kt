package com.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "menu_options",
    primaryKeys = ["menuId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = MenuDetailEntity::class,
            parentColumns = ["menuId"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("menuId")],
)
data class MenuOptionEntity(
    val menuId: Long,
    val id: Long,
    val groupName: String,
    val name: String,
    val additionalPrice: Int,
    val isDefault: Boolean,
)
