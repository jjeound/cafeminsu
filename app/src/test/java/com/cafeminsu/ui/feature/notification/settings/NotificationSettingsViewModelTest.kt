package com.cafeminsu.ui.feature.notification.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val mainDispatcherRule = NotificationSettingsMainDispatcherRule()

    @Test
    fun observesPreferenceDefaults() = runTest {
        val viewModel = NotificationSettingsViewModel(createPreferences())

        viewModel.uiState.test {
            val state = awaitState { true }

            assertTrue(state.orderStatusEnabled)
            assertTrue(state.promotionEnabled)
            assertFalse(state.marketingEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleUpdatesStateAndPersists() = runTest {
        val preferences = createPreferences()
        val viewModel = NotificationSettingsViewModel(preferences)

        viewModel.uiState.test {
            awaitState { true }

            viewModel.onToggle(NotificationCategory.Marketing, enabled = true)
            viewModel.onToggle(NotificationCategory.OrderStatus, enabled = false)

            val updated = awaitState { it.marketingEnabled && !it.orderStatusEnabled }
            assertTrue(updated.marketingEnabled)
            assertFalse(updated.orderStatusEnabled)

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(true, preferences.observeMarketingNotification().first())
        assertEquals(false, preferences.observeOrderStatusNotification().first())
    }

    private suspend fun ReceiveTurbine<NotificationSettingsUiState>.awaitState(
        predicate: (NotificationSettingsUiState) -> Boolean,
    ): NotificationSettingsUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    private fun TestScope.createPreferences(): UserPreferencesDataStore =
        UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
                produceFile = { File(tempFolder.newFolder(), "user.preferences_pb") },
            ),
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
