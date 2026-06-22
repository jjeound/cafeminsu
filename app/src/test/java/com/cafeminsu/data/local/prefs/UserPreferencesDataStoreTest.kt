package com.cafeminsu.data.local.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun unsetKeysReturnDefaults() = runTest {
        val prefs = createDataStore()

        assertNull(prefs.observeSelectedStore().first())
        assertEquals(false, prefs.observeOwnerStoreOpen().first())
        assertEquals(false, prefs.observeOnboardingShown().first())
        assertNull(prefs.observeLastCustomerTab().first())
        assertNull(prefs.readOwnerStoreOpenOrNull())
    }

    @Test
    fun storedValuesAreEmitted() = runTest {
        val prefs = createDataStore()

        prefs.setSelectedStore("""{"id":"7"}""")
        prefs.setOwnerStoreOpen(true)
        prefs.setOnboardingShown(true)
        prefs.setLastCustomerTab("home")

        assertEquals("""{"id":"7"}""", prefs.observeSelectedStore().first())
        assertEquals(true, prefs.observeOwnerStoreOpen().first())
        assertEquals(true, prefs.observeOnboardingShown().first())
        assertEquals("home", prefs.observeLastCustomerTab().first())
        assertEquals(true, prefs.readOwnerStoreOpenOrNull())
    }

    @Test
    fun overwriteReplacesPreviousValue() = runTest {
        val prefs = createDataStore()

        prefs.setLastCustomerTab("home")
        prefs.setLastCustomerTab("my")

        assertEquals("my", prefs.observeLastCustomerTab().first())
    }

    @Test
    fun settingNullClearsStoredValue() = runTest {
        val prefs = createDataStore()
        prefs.setSelectedStore("""{"id":"7"}""")

        prefs.setSelectedStore(null)

        assertNull(prefs.observeSelectedStore().first())
        assertNull(prefs.observeLastCustomerTab().first())
    }

    private fun TestScope.createDataStore(): UserPreferencesDataStore =
        UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job()),
                produceFile = { File(tempFolder.newFolder(), "user.preferences_pb") },
            ),
        )
}
