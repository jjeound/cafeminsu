package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.SelectedOption
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<AppResult<Cart>>
    suspend fun addItem(menuItemId: String, options: List<SelectedOption>, quantity: Int): AppResult<Cart>
    suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart>
    suspend fun removeItem(cartItemId: String): AppResult<Cart>
    suspend fun validateForCheckout(): AppResult<CartValidation>
    suspend fun clear(): AppResult<Unit>
}
