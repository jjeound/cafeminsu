package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockOwnerOrderRepositoryTest {
    @Test
    fun seededOrdersProduceOwnerDashboardStats() = runBlocking {
        val repository = MockOwnerOrderRepository()

        repository.observeIncomingOrders().test {
            val orders = awaitItem().successData()

            assertEquals(37, orders.size)
            assertEquals(482_000, orders.sumOf { it.totalAmount })
            assertEquals(3, orders.count { it.status == OrderStatus.Accepted })
            assertEquals(5, orders.count { it.status == OrderStatus.Preparing })
            assertEquals(2, orders.count { it.status == OrderStatus.Ready })
            assertTrue(orders.any { it.orderNumber == "1042" })
            assertTrue(orders.any { it.orderNumber == "1041" })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun advanceStatusUpdatesObservedOrders() = runBlocking {
        val repository = MockOwnerOrderRepository()
        val firstNewOrder = repository.observeIncomingOrders()
            .firstSuccess()
            .first { it.status == OrderStatus.Accepted }

        repository.observeIncomingOrders().test {
            awaitItem()

            val result = repository.advanceStatus(firstNewOrder.id, OrderStatus.Preparing)

            assertEquals(OrderStatus.Preparing, result.successData().status)
            val updatedOrders = awaitItem().successData()
            assertEquals(
                OrderStatus.Preparing,
                updatedOrders.first { it.id == firstNewOrder.id }.status,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun advanceStatusReturnsNotFoundForMissingOrder() = runBlocking {
        val repository = MockOwnerOrderRepository()

        val result = repository.advanceStatus("missing-order", OrderStatus.Preparing)

        assertTrue(result is AppResult.Failure)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }

    private suspend fun kotlinx.coroutines.flow.Flow<AppResult<List<Order>>>.firstSuccess(): List<Order> {
        var orders = emptyList<Order>()
        test {
            orders = awaitItem().successData()
            cancelAndIgnoreRemainingEvents()
        }
        return orders
    }
}
