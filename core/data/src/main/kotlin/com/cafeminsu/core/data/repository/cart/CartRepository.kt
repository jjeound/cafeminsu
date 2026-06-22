package com.cafeminsu.core.data.repository.cart

import com.cafeminsu.core.model.cart.Cart
import com.cafeminsu.core.model.cart.CartValidation
import com.cafeminsu.core.model.cart.SelectedOption
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>

    fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int
    ): Flow<Cart>

    fun updateItemQuantity(
        cartItemId: String,
        quantity: Int
    ): Flow<Cart>

    fun removeItem(cartItemId: String): Flow<Cart>

    fun validateForCheckout(): Flow<CartValidation>

    fun clear(): Flow<Unit>
}
