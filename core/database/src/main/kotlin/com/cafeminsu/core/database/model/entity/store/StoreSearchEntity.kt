package com.cafeminsu.core.database.model.entity.store

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "store_search_results",
    primaryKeys = ["query", "page", "position"],
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("storeId")],
)
data class StoreSearchEntity(
    val query: String,
    val page: Int,
    val position: Int,
    val storeId: Long,
)
