package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.repository.GiftRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGiftRepository(
    private val nowMillis: () -> Long,
) : GiftRepository {
    @Inject
    constructor() : this(nowMillis = { System.currentTimeMillis() })

    private var nextGiftSequence = 1

    override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> {
        val validationError = validate(request)
        if (validationError != null) {
            return AppResult.Failure(validationError)
        }

        val result = GiftSendResult(
            giftId = "gift-${nextGiftSequence++}",
            sentAtMillis = nowMillis(),
        )
        return AppResult.Success(result)
    }

    private fun validate(request: GiftSendRequest): DomainError? =
        when {
            request.amount <= 0 -> DomainError.Validation("amount")
            request.recipientRef.isBlank() -> DomainError.Validation("recipient")
            request.message != null && request.message.length > MaxMessageLength ->
                DomainError.Validation("message")

            else -> null
        }

    private companion object {
        const val MaxMessageLength = 100
    }
}
