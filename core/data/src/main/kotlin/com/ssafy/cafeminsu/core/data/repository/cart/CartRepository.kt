package com.ssafy.cafeminsu.core.data.repository.cart

import com.ssafy.cafeminsu.core.model.cart.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartByStoreId(storeId: Long): Flow<Cart>

    suspend fun updateCart(
        storeId: Long,
        cart: Cart,
    )

    suspend fun clear()
}
