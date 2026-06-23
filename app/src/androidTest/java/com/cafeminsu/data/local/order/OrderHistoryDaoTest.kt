package com.cafeminsu.data.local.order

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cafeminsu.data.local.db.CafeDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderHistoryDaoTest {
    private lateinit var database: CafeDatabase
    private lateinit var dao: OrderHistoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CafeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.orderHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAllThenGetAllReturnsNewestFirst() = runTest {
        dao.upsertAll(
            listOf(
                orderEntity("77", createdAtMillis = 10L),
                orderEntity("78", createdAtMillis = 30L),
                orderEntity("79", createdAtMillis = 20L),
            ),
        )

        assertEquals(listOf("78", "79", "77"), dao.getAll().map { it.id })
    }

    @Test
    fun upsertReplacesRowWithSameId() = runTest {
        dao.upsertAll(listOf(orderEntity("77", orderNumber = "A-1000")))
        dao.upsertAll(listOf(orderEntity("77", orderNumber = "A-2000")))

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("A-2000", rows.single().orderNumber)
    }

    @Test
    fun clearRemovesAllRows() = runTest {
        dao.upsertAll(listOf(orderEntity("77")))

        dao.clear()

        assertTrue(dao.getAll().isEmpty())
    }

    private fun orderEntity(
        id: String,
        orderNumber: String = "A-$id",
        createdAtMillis: Long = 0L,
    ): OrderEntity =
        OrderEntity(
            id = id,
            orderNumber = orderNumber,
            totalAmount = 10_000,
            status = "Completed",
            createdAtMillis = createdAtMillis,
            itemsJson = "[]",
        )
}
