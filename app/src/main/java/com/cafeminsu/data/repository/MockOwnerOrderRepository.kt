package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.OwnerOrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockOwnerOrderRepository @Inject constructor() : OwnerOrderRepository {
    private val orders = MutableStateFlow(seedOrders())

    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        orders.map { currentOrders ->
            val filteredOrders = if (filter == null) {
                currentOrders
            } else {
                currentOrders.filter { it.status == filter }
            }
            AppResult.Success(filteredOrders)
        }

    override suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> {
        val currentOrders = orders.value
        val order = currentOrders.firstOrNull { it.id == orderId }
            ?: return AppResult.Failure(DomainError.NotFound)
        if (!order.status.canAdvanceTo(to)) {
            return AppResult.Failure(DomainError.Validation("status"))
        }

        val updatedOrder = order.copy(status = to)
        orders.value = currentOrders.map { currentOrder ->
            if (currentOrder.id == orderId) updatedOrder else currentOrder
        }
        return AppResult.Success(updatedOrder)
    }

    private fun OrderStatus.canAdvanceTo(next: OrderStatus): Boolean =
        when (this) {
            OrderStatus.Accepted -> next == OrderStatus.Preparing
            OrderStatus.Preparing -> next == OrderStatus.Ready
            OrderStatus.Ready -> next == OrderStatus.Completed
            OrderStatus.PendingPayment,
            OrderStatus.Paid,
            OrderStatus.Completed,
            OrderStatus.Cancelled,
            OrderStatus.Failed,
            -> false
        }

    private fun seedOrders(): List<Order> {
        val visibleQueue = listOf(
            ownerOrder(
                id = "owner-order-1042",
                orderNumber = "1042",
                itemName = "아메리카노(L) ICE",
                itemCount = 2,
                totalAmount = 9_300,
                status = OrderStatus.Accepted,
                createdAtMillis = TodayAt1414Millis,
            ),
            ownerOrder(
                id = "owner-order-1041",
                orderNumber = "1041",
                itemName = "카페라떼(R) HOT",
                itemCount = 2,
                totalAmount = 11_000,
                status = OrderStatus.Preparing,
                createdAtMillis = TodayAt1409Millis,
            ),
            ownerOrder(
                id = "owner-order-1040",
                orderNumber = "1040",
                itemName = "바닐라라떼(R) ICE",
                itemCount = 1,
                totalAmount = 5_500,
                status = OrderStatus.Accepted,
                createdAtMillis = TodayAt1403Millis,
            ),
            ownerOrder(
                id = "owner-order-1039",
                orderNumber = "1039",
                itemName = "콜드브루(R) ICE",
                itemCount = 1,
                totalAmount = 6_200,
                status = OrderStatus.Accepted,
                createdAtMillis = TodayAt1358Millis,
            ),
        )
        val completedOrders = (1..CompletedSeedOrderCount).map { index ->
            ownerOrder(
                id = "owner-order-${1000 + index}",
                orderNumber = (1000 + index).toString(),
                itemName = if (index % 2 == 0) "아메리카노(R) ICE" else "헤이즐넛라떼(R) HOT",
                itemCount = 1,
                totalAmount = completedSeedAmount(index),
                status = OrderStatus.Completed,
                createdAtMillis = TodayAt1200Millis - index * TenMinutesMillis,
            )
        }

        return visibleQueue + completedOrders
    }

    private fun completedSeedAmount(index: Int): Int =
        if (index == CompletedSeedOrderCount) {
            LastCompletedAmount
        } else {
            StandardCompletedAmount
        }

    private fun ownerOrder(
        id: String,
        orderNumber: String,
        itemName: String,
        itemCount: Int,
        totalAmount: Int,
        status: OrderStatus,
        createdAtMillis: Long,
    ): Order =
        Order(
            id = id,
            orderNumber = orderNumber,
            items = ownerItems(orderId = id, firstName = itemName, itemCount = itemCount),
            totalAmount = totalAmount,
            status = status,
            createdAtMillis = createdAtMillis,
        )

    private fun ownerItems(
        orderId: String,
        firstName: String,
        itemCount: Int,
    ): List<CartItem> {
        val firstItem = CartItem(
            id = "$orderId-item-1",
            menuItemId = "menu-$orderId-1",
            name = firstName,
            unitPrice = 4_500,
            selectedOptions = listOf(
                SelectedOption(
                    groupId = "temperature",
                    optionId = if (firstName.contains("ICE")) "ice" else "hot",
                    name = if (firstName.contains("ICE")) "ICE" else "HOT",
                    extraPrice = 0,
                ),
            ),
            quantity = 1,
        )
        if (itemCount == 1) return listOf(firstItem)

        return listOf(
            firstItem,
            CartItem(
                id = "$orderId-item-2",
                menuItemId = "menu-$orderId-2",
                name = "바닐라라떼(R) HOT",
                unitPrice = 5_500,
                selectedOptions = emptyList(),
                quantity = 1,
            ),
        )
    }

    private companion object {
        const val CompletedSeedOrderCount = 33
        const val StandardCompletedAmount = 13_500
        const val LastCompletedAmount = 18_000
        const val TenMinutesMillis = 10L * 60L * 1000L

        const val TodayAt1414Millis = 1_750_311_240_000L
        const val TodayAt1409Millis = 1_750_310_940_000L
        const val TodayAt1403Millis = 1_750_310_580_000L
        const val TodayAt1358Millis = 1_750_310_280_000L
        const val TodayAt1200Millis = 1_750_303_200_000L
    }
}
