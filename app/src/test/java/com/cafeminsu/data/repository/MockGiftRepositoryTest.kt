package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGiftRepositoryTest {
    @Test
    fun sendGiftValidatesInputAndReturnsResultWithoutPersistingRecipient() = runBlocking {
        val repository = MockGiftRepository(nowMillis = { 1_803_974_400_000L })

        val result = repository.sendGift(
            GiftSendRequest(
                amount = 10_000,
                channel = GiftChannel.KakaoTalk,
                recipientRef = "friend-sensitive-id",
                message = "오늘 하루 수고 많았어",
            ),
        )
        val invalid = repository.sendGift(
            GiftSendRequest(
                amount = 0,
                channel = GiftChannel.Sms,
                recipientRef = "010-1234-5678",
                message = null,
            ),
        )

        assertTrue(result is AppResult.Success)
        assertTrue(invalid is AppResult.Failure)
    }
}
