package com.ssafy.cafeminsu.core.database.model.entity.menu

import androidx.room.Embedded
import androidx.room.Relation

data class MenuWithOptions(
    @Embedded
    val menu: MenuEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "menuId",
    )
    val options: List<MenuOptionEntity>,
)