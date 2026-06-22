package com.cafeminsu.data.local.notification

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
class NotificationDaoTest {
    private lateinit var database: CafeDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CafeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.notificationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAllThenGetAllReturnsNewestFirst() = runTest {
        dao.upsertAll(
            listOf(
                notificationEntity("71", createdAtMillis = 10L),
                notificationEntity("72", createdAtMillis = 30L),
                notificationEntity("73", createdAtMillis = 20L),
            ),
        )

        assertEquals(listOf("72", "73", "71"), dao.getAll().map { it.id })
    }

    @Test
    fun upsertReplacesRowWithSameId() = runTest {
        dao.upsertAll(listOf(notificationEntity("71", title = "이전")))
        dao.upsertAll(listOf(notificationEntity("71", title = "새 알림")))

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("새 알림", rows.single().title)
    }

    @Test
    fun clearRemovesAllRows() = runTest {
        dao.upsertAll(listOf(notificationEntity("71")))

        dao.clear()

        assertTrue(dao.getAll().isEmpty())
    }

    private fun notificationEntity(
        id: String,
        title: String = "알림 $id",
        createdAtMillis: Long = 0L,
    ): NotificationEntity =
        NotificationEntity(
            id = id,
            type = "OrderReady",
            title = title,
            body = "본문 $id",
            createdAtMillis = createdAtMillis,
            read = false,
        )
}
