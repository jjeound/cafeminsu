package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftRepositoryTest {
    @Test
    fun exposesGiftRepositoryContract() = runBlocking {
        val repository = object : GiftRepository {
            override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> =
                AppResult.Success(
                    GiftSendResult(
                        giftId = "gift-1",
                        sentAtMillis = 1_803_974_400_000L,
                    ),
                )

            override suspend fun claimGift(
                claimCode: String,
            ): AppResult<com.cafeminsu.domain.model.Gifticon> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)
        }

        val result = repository.sendGift(
            GiftSendRequest(
                amount = 10_000,
                channel = GiftChannel.KakaoTalk,
                recipientRef = "friend-1",
                message = "오늘 하루 수고 많았어",
            ),
        )

        assertTrue(result is AppResult.Success)
    }
}
