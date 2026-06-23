package com.cafeminsu.data.local.menu

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
class MenuDaoTest {
    private lateinit var database: CafeDatabase
    private lateinit var dao: MenuDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CafeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.menuDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAllThenByStoreReturnsOnlyMatchingStore() = runTest {
        dao.upsertAll(listOf(menuEntity("101", storeId = "11"), menuEntity("102", storeId = "22")))

        assertEquals(setOf("101"), dao.byStore("11").map { it.id }.toSet())
    }

    @Test
    fun upsertReplacesRowWithSameId() = runTest {
        dao.upsertAll(listOf(menuEntity("101", name = "이전 이름")))
        dao.upsertAll(listOf(menuEntity("101", name = "새 이름")))

        val rows = dao.byStore("11")
        assertEquals(1, rows.size)
        assertEquals("새 이름", rows.single().name)
    }

    @Test
    fun clearStoreRemovesOnlyThatStore() = runTest {
        dao.upsertAll(listOf(menuEntity("101", storeId = "11"), menuEntity("102", storeId = "22")))

        dao.clearStore("11")

        assertTrue(dao.byStore("11").isEmpty())
        assertEquals(setOf("102"), dao.byStore("22").map { it.id }.toSet())
    }

    private fun menuEntity(
        id: String,
        storeId: String = "11",
        name: String = "메뉴 $id",
    ): MenuEntity =
        MenuEntity(
            id = id,
            storeId = storeId,
            categoryId = "커피",
            name = name,
            description = "",
            basePrice = 5_500,
            imageUrl = null,
            isSoldOut = false,
            isVisible = true,
            optionsJson = "[]",
        )
}
