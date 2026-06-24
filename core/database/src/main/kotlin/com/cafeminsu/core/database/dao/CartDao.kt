package com.cafeminsu.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cafeminsu.core.database.model.entity.cart.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE storeId = :storeId ORDER BY createdAtMillis ASC")
    fun getCartItemEntities(storeId: Long): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItemEntity(cartItemEntity: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItemEntities(cartItemEntities: List<CartItemEntity>)

    @Update
    suspend fun updateCartItemEntity(cartItemEntity: CartItemEntity)

    @Delete
    suspend fun deleteCartItemEntity(cartItemEntity: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :cartItemId")
    suspend fun deleteCartItemEntity(cartItemId: String)

    @Query("DELETE FROM cart_items WHERE storeId = :storeId")
    suspend fun clearStoreCart(storeId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearAllCart()
}
