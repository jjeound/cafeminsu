package com.ssafy.cafeminsu.core.database.model.entity.menu

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_options",
    foreignKeys = [
        ForeignKey(
            entity = MenuEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("menuId"),
    ],
)
data class MenuOptionEntity(
    @PrimaryKey
    val id: Long,
    val menuId: Long,
    val groupName: String,
    val name: String,
    val additionalPrice: Int,
    val isDefault: Boolean,
)
