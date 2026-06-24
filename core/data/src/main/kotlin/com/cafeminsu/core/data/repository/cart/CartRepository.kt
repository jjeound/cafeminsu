package com.cafeminsu.core.data.repository.cart

import com.cafeminsu.core.model.cart.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartByStoreId(storeId: Long): Flow<Cart>

    suspend fun updateCart(
        storeId: Long,
        cart: Cart,
    )

    suspend fun clear()
}
