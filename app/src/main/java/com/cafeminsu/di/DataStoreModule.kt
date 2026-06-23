package com.cafeminsu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> =
        createUserPreferencesDataStore(scope) {
            context.preferencesDataStoreFile(UserPreferencesFileName)
        }
}

internal const val UserPreferencesFileName = "cafeminsu_user_prefs"

internal fun createUserPreferencesDataStore(
    scope: CoroutineScope,
    produceFile: () -> File,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = scope, produceFile = produceFile)
