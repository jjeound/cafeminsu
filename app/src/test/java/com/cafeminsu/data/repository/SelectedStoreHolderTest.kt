package com.cafeminsu.data.repository

import app.cash.turbine.test
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import com.squareup.moshi.Moshi
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedStoreHolderTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun selectedStoreStartsEmptyAndEmitsUpdates() = runTest {
        val holder = SelectedStoreHolder(newPreferences(), moshi, backgroundScope)

        holder.observe().test {
            assertNull(awaitItem())

            holder.select(sampleStore(id = "7"))

            assertEquals("7", awaitItem()?.id)
            assertEquals("7", holder.current()?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newInstanceRehydratesPersistedStore() {
        val preferences = newPreferences()
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val first = SelectedStoreHolder(preferences, moshi, scope)
        first.select(sampleStore(id = "42"))

        // 새 인스턴스(앱 재시작 모사)가 같은 DataStore 에서 선택 매장을 복원해야 한다.
        val second = SelectedStoreHolder(preferences, moshi, scope)

        assertEquals("42", second.current()?.id)
        assertEquals("42", second.observe().value?.id)
    }

    @Test
    fun brokenJsonFallsBackToNull() {
        val preferences = newPreferences()
        runBlocking { preferences.setSelectedStore("not-a-valid-store-json") }

        val holder = SelectedStoreHolder(preferences, moshi, CoroutineScope(Dispatchers.Unconfined))

        assertNull(holder.current())
        assertNull(holder.observe().value)
    }

    private fun newPreferences(): UserPreferencesDataStore =
        UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.Unconfined),
                produceFile = { File(Files.createTempDirectory("selected_store_prefs").toFile(), "user.preferences_pb") },
            ),
        )

    private fun sampleStore(id: String): Store =
        Store(
            id = id,
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = emptyList(),
        )
}
