package com.cafeminsu.data.local.store

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
class StoreDaoTest {
    private lateinit var database: CafeDatabase
    private lateinit var dao: StoreDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CafeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.storeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAllThenGetAllRoundTrips() = runTest {
        dao.upsertAll(listOf(storeEntity("7"), storeEntity("8")))

        assertEquals(setOf("7", "8"), dao.getAll().map { it.id }.toSet())
    }

    @Test
    fun upsertReplacesRowWithSameId() = runTest {
        dao.upsertAll(listOf(storeEntity("7", name = "이전 이름")))
        dao.upsertAll(listOf(storeEntity("7", name = "새 이름")))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("새 이름", all.single().name)
    }

    @Test
    fun clearRemovesAllRows() = runTest {
        dao.upsertAll(listOf(storeEntity("7")))

        dao.clear()

        assertTrue(dao.getAll().isEmpty())
    }

    private fun storeEntity(id: String, name: String = "카페민수 $id"): StoreEntity =
        StoreEntity(
            id = id,
            name = name,
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = "Open",
            closingTimeLabel = null,
            amenities = "Wifi",
        )
}
