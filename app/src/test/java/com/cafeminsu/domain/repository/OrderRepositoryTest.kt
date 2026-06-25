package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class OrderRepositoryTest {
    @Test
    fun exposesOrderRepositoryContract() = runBlocking {
        val repository = object : OrderRepository {
            override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
                flowOf(AppResult.Failure(com.cafeminsu.core.DomainError.NotFound))

            override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
                flowOf(AppResult.Success(emptyList()))

            override fun observeRecentOrders(): Flow<AppResult<List<Order>>> =
                flowOf(AppResult.Success(emptyList()))
        }

        val orderHistory: Flow<AppResult<List<Order>>> = repository.observeOrderHistory()
        assertNotNull(orderHistory)
        assertNotNull(repository.observeRecentOrders())
    }
}
