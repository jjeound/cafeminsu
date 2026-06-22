package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockOrderRepositoryTest {
    @Test
    fun createOrderFromCartCreatesPendingPaymentOrder() = runBlocking {
        val repository = MockOrderRepository()
        val cart = cart()

        val order = repository.createOrderFromCart(cart).successData()

        assertEquals(OrderStatus.PendingPayment, order.status)
        assertEquals(cart.subtotal, order.totalAmount)
        assertTrue(order.orderNumber.isNotBlank())
    }

    @Test
    fun observeOrderAndHistoryExposeCreatedOrder() = runBlocking {
        val repository = MockOrderRepository()
        val order = repository.createOrderFromCart(cart()).successData()

        repository.observeOrder(order.id).test {
            assertEquals(order, awaitItem().successData())
            cancelAndIgnoreRemainingEvents()
        }

        repository.observeOrderHistory().test {
            assertEquals(listOf(order), awaitItem().successData())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun cart(): Cart {
        val menu = MockData.menuItems.first { !it.isSoldOut }
        val item = CartItem(
            id = "cart-test-1",
            menuItemId = menu.id,
            name = menu.name,
            unitPrice = menu.basePrice,
            selectedOptions = emptyList(),
            quantity = 3,
        )
        return Cart(
            items = listOf(item),
            subtotal = item.unitPrice * item.quantity,
            validation = CartValidation.Valid,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}
