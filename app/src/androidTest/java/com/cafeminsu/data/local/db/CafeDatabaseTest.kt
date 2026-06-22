package com.cafeminsu.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CafeDatabaseTest {
    @Test
    fun buildsAndExposesStoreDao() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, CafeDatabase::class.java).build()

        assertNotNull(database.storeDao())

        database.close()
    }
}
