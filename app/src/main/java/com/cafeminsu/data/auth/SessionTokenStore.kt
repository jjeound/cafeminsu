package com.cafeminsu.data.auth

interface SessionTokenStore {
    suspend fun read(): SessionTokens?
    suspend fun save(tokens: SessionTokens)
    suspend fun updateAccessToken(accessToken: String)
    suspend fun clear()
}

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
) {
    override fun toString(): String = "SessionTokens(accessToken=<redacted>, refreshToken=<redacted>)"
}
