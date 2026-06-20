package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import javax.inject.Inject
import javax.inject.Singleton

interface PgClient {
    suspend fun authorize(
        merchantUid: String,
        amount: Int,
    ): AppResult<String>
}

@Singleton
class MockPgClient @Inject constructor() : PgClient {
    override suspend fun authorize(
        merchantUid: String,
        amount: Int,
    ): AppResult<String> =
        when {
            merchantUid.isBlank() -> AppResult.Failure(DomainError.Validation("merchantUid"))
            amount <= 0 -> AppResult.Failure(DomainError.Validation("amount"))
            merchantUid.contains(MockFailureMarker, ignoreCase = true) -> {
                AppResult.Success("imp_mock_fail_$merchantUid")
            }

            else -> AppResult.Success("imp_mock_$merchantUid")
        }

    private companion object {
        const val MockFailureMarker = "fail"
    }
}
