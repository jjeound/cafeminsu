package com.cafeminsu.domain.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerOrderRepositoryTest {
    @Test
    fun ownerOrderRepositoryContractObservesAndAdvancesOrders() = runBlocking {
        val repository = FakeOwnerOrderRepository(
            initialOrders = listOf(sampleOrder(status = OrderStatus.Accepted)),
        )

        repository.observeIncomingOrders().test {
            assertEquals(OrderStatus.Accepted, awaitItem().successData().single().status)

            val updatedOrder = repository.advanceStatus("order-1", OrderStatus.Preparing).successData()

            assertEquals(OrderStatus.Preparing, updatedOrder.status)
            assertEquals(OrderStatus.Preparing, awaitItem().successData().single().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ownerOrderRepositoryContractSupportsStatusFilter() = runBlocking {
        val repository = FakeOwnerOrderRepository(
            initialOrders = listOf(
                sampleOrder(id = "order-1", status = OrderStatus.Accepted),
                sampleOrder(id = "order-2", status = OrderStatus.Preparing),
            ),
        )

        repository.observeIncomingOrders(filter = OrderStatus.Preparing).test {
            assertEquals(listOf("order-2"), awaitItem().successData().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}

private class FakeOwnerOrderRepository(
    initialOrders: List<Order>,
) : OwnerOrderRepository {
    private val orders = MutableStateFlow(initialOrders)

    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        orders.map { currentOrders ->
            AppResult.Success(
                if (filter == null) {
                    currentOrders
                } else {
                    currentOrders.filter { it.status == filter }
                },
            )
        }

    override suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> {
        val updatedOrder = orders.value.first { it.id == orderId }.copy(status = to)
        orders.value = orders.value.map { order ->
            if (order.id == orderId) updatedOrder else order
        }
        return AppResult.Success(updatedOrder)
    }
}

private fun sampleOrder(
    id: String = "order-1",
    status: OrderStatus,
): Order =
    Order(
        id = id,
        orderNumber = "1042",
        items = listOf(
            CartItem(
                id = "$id-item",
                menuItemId = "americano",
                name = "아메리카노(L) ICE",
                unitPrice = 4_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        ),
        totalAmount = 4_500,
        status = status,
        createdAtMillis = 1_750_311_240_000L,
    )
