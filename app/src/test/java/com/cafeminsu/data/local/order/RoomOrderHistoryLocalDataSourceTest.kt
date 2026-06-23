package com.cafeminsu.data.local.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomOrderHistoryLocalDataSourceTest {
    private val dao = mockk<OrderHistoryDao>(relaxed = true)
    private val moshi = Moshi.Builder().build()
    private val dataSource = RoomOrderHistoryLocalDataSource(dao, moshi)

    @Test
    fun cachedHistoryMapsDaoEntitiesToDomainIncludingItems() = runTest {
        val order = sampleOrder()
        coEvery { dao.getAll() } returns listOf(order.toOrderEntity(moshi))

        assertEquals(listOf(order), dataSource.cachedHistory())
    }

    @Test
    fun replaceHistoryClearsThenUpsertsMappedEntities() = runTest {
        val captured = slot<List<OrderEntity>>()
        coEvery { dao.upsertAll(capture(captured)) } returns Unit

        dataSource.replaceHistory(listOf(sampleOrder()))

        // 사라진 주문이 남지 않도록 전체를 비운 뒤 다시 채우는 순서를 보장한다.
        coVerifyOrder {
            dao.clear()
            dao.upsertAll(any())
        }
        assertEquals("77", captured.captured.single().id)
        assertEquals("Completed", captured.captured.single().status)
    }

    private fun sampleOrder(): Order =
        Order(
            id = "77",
            orderNumber = "A-2543",
            items = listOf(
                CartItem(
                    id = "cart-item-1",
                    menuItemId = "101",
                    name = "바닐라라떼",
                    unitPrice = 6_000,
                    selectedOptions = listOf(
                        SelectedOption(groupId = "온도", optionId = "1", name = "ICE", extraPrice = 0),
                    ),
                    quantity = 2,
                ),
            ),
            totalAmount = 12_000,
            status = OrderStatus.Completed,
            createdAtMillis = 1_700_000_000_000L,
        )
}
