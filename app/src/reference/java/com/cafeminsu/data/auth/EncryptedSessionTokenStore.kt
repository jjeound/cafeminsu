package com.cafeminsu.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EncryptedSessionTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionTokenStore {
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            TokenStoreName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun read(): SessionTokens? = withContext(Dispatchers.IO) {
        val accessToken = preferences.getString(AccessTokenKey, null)
        val refreshToken = preferences.getString(RefreshTokenKey, null)

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            null
        } else {
            SessionTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }
    }

    override suspend fun save(tokens: SessionTokens) {
        withContext(Dispatchers.IO) {
            preferences.edit()
                .putString(AccessTokenKey, tokens.accessToken)
                .putString(RefreshTokenKey, tokens.refreshToken)
                .apply()
        }
    }

    override suspend fun updateAccessToken(accessToken: String) {
        withContext(Dispatchers.IO) {
            preferences.edit()
                .putString(AccessTokenKey, accessToken)
                .apply()
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            preferences.edit().clear().apply()
        }
    }

    private companion object {
        const val TokenStoreName = "cafeminsu_secure_session"
        const val AccessTokenKey = "access_token"
        const val RefreshTokenKey = "refresh_token"
    }
}
