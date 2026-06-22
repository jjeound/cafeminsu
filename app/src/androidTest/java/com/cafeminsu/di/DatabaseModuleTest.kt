package com.cafeminsu.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseModuleTest {
    @Test
    fun providesDatabaseDaoAndLocalDataSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val database = DatabaseModule.provideCafeDatabase(context)
        val dao = DatabaseModule.provideStoreDao(database)

        assertNotNull(dao)
        assertNotNull(DatabaseModule.provideStoreLocalDataSource(dao))

        database.close()
    }
}
