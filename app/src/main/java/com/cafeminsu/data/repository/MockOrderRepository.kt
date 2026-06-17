package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockOrderRepository(
    private val nowMillis: () -> Long,
) : OrderRepository {
    @Inject
    constructor() : this(nowMillis = { System.currentTimeMillis() })

    private val orders = MutableStateFlow<List<Order>>(emptyList())
    private var nextOrderNumber = 1

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> {
        if (cart.items.isEmpty() || cart.validation != CartValidation.Valid) {
            return AppResult.Failure(DomainError.Validation("cart"))
        }

        val orderNumber = nextOrderNumber++
        val order = Order(
            id = "order-$orderNumber",
            orderNumber = "M${orderNumber.toString().padStart(3, '0')}",
            items = cart.items,
            totalAmount = cart.subtotal,
            status = OrderStatus.PendingPayment,
            createdAtMillis = nowMillis(),
        )
        orders.value = orders.value + order
        return AppResult.Success(order)
    }

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        orders.map { currentOrders ->
            val order = currentOrders.firstOrNull { it.id == orderId }
            if (order == null) {
                AppResult.Failure(DomainError.NotFound)
            } else {
                AppResult.Success(order)
            }
        }

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        orders.map { AppResult.Success(it) }
}
