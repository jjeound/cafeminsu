package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.SelectedOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CartRepositoryTest {
    @Test
    fun exposesCartRepositoryContract() = runBlocking {
        val repository = object : CartRepository {
            override fun observeCart(): Flow<AppResult<Cart>> =
                flowOf(AppResult.Failure(com.cafeminsu.core.DomainError.Unknown))

            override suspend fun addItem(
                menuItemId: String,
                options: List<SelectedOption>,
                quantity: Int,
            ): AppResult<Cart> = AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override suspend fun removeItem(cartItemId: String): AppResult<Cart> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override suspend fun validateForCheckout(): AppResult<CartValidation> =
                AppResult.Success(CartValidation.Valid)

            override suspend fun clear(): AppResult<Unit> =
                AppResult.Success(Unit)
        }

        assertTrue(repository.validateForCheckout() is AppResult.Success)
        assertTrue(repository.clear() is AppResult.Success)
    }
}
