package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.repository.FcmTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockFcmTokenRepository @Inject constructor() : FcmTokenRepository {
    private val tokens = mutableListOf<String>()

    val registeredTokens: List<String>
        get() = tokens.toList()

    override suspend fun register(token: String): AppResult<Unit> {
        tokens += token
        return AppResult.Success(Unit)
    }
}
