package com.ssafy.cafeminsu.core.database.model.relation.menu

import androidx.room.Embedded
import androidx.room.Relation
import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuOptionEntity

data class MenuWithOptions(
    @Embedded val menu: MenuEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "menuId",
    )
    val options: List<MenuOptionEntity>,
)
