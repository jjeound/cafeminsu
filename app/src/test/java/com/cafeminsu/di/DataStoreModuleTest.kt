package com.cafeminsu.di

import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreModuleTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun createUserPreferencesDataStorePersistsValues() = runTest {
        val file = File(tempFolder.newFolder(), "user.preferences_pb")
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

        val prefs = UserPreferencesDataStore(
            createUserPreferencesDataStore(scope) { file },
        )
        prefs.setOnboardingShown(true)

        assertEquals(true, prefs.observeOnboardingShown().first())
    }
}
