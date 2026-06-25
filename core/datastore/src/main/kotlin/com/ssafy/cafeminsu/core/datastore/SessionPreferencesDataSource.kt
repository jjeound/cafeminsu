package com.ssafy.cafeminsu.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class StoredSessionTokens(
    val accessToken: String,
    val refreshToken: String,
)

class SessionPreferencesDataSource @Inject constructor(
    private val sessionTokens: DataStore<SessionTokensProto>,
) {
    val tokens: Flow<StoredSessionTokens> = sessionTokens.data.map {
        StoredSessionTokens(accessToken = it.accessToken, refreshToken = it.refreshToken)
    }

    suspend fun setTokens(accessToken: String, refreshToken: String) {
        sessionTokens.updateData {
            it.toBuilder().setAccessToken(accessToken).setRefreshToken(refreshToken).build()
        }
    }

    suspend fun setAccessToken(accessToken: String) {
        sessionTokens.updateData { it.toBuilder().setAccessToken(accessToken).build() }
    }

    suspend fun clearTokens() {
        sessionTokens.updateData { SessionTokensProto.getDefaultInstance() }
    }
}
