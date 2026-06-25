package com.ssafy.cafeminsu.core.database.model.entity.cart

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cart_items",
    indices = [Index("storeId")],
)
data class CartItemEntity(
    @PrimaryKey val id: String,
    val storeId: Long,
    val menuId: Long,
    val name: String,
    val imageUrl: String?,
    val price: Int,
    val selectedOptionsJson: String,
    val quantity: Int,
    val createdAtMillis: Long,
)
