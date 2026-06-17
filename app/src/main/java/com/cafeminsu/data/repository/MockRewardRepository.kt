package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.StampEvent
import com.cafeminsu.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockRewardRepository(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : RewardRepository {
    private val stampCard = MutableStateFlow(MockData.initialStampCard)
    private val gifticons = MutableStateFlow(MockData.initialGifticons)

    override fun observeStampCard(): Flow<AppResult<StampCard>> =
        stampCard.map { AppResult.Success(it) }

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> {
        if (orderId.isBlank()) {
            return AppResult.Failure(DomainError.Validation("orderId"))
        }

        val current = stampCard.value
        val earnedCount = current.currentCount + 1
        val reachedGoal = earnedCount >= current.goalCount
        val event = StampEvent(
            id = "stamp-${current.history.size + 1}",
            orderId = orderId,
            count = 1,
            createdAtMillis = nowMillis(),
        )
        val updated = current.copy(
            currentCount = if (reachedGoal) earnedCount % current.goalCount else earnedCount,
            history = listOf(event) + current.history,
        )
        stampCard.value = updated

        if (reachedGoal) {
            gifticons.value = gifticons.value + earnedGifticon(gifticons.value.size + 1)
        }

        return AppResult.Success(updated)
    }

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        gifticons.map { AppResult.Success(it) }

    override suspend fun getGifticon(id: String): AppResult<Gifticon> {
        val gifticon = gifticons.value.firstOrNull { it.id == id }
        return if (gifticon == null) {
            AppResult.Failure(DomainError.NotFound)
        } else {
            AppResult.Success(gifticon)
        }
    }

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> {
        val currentGifticons = gifticons.value
        val gifticon = currentGifticons.firstOrNull { it.id == id }
            ?: return AppResult.Failure(DomainError.NotFound)
        val used = gifticon.copy(status = GifticonStatus.Used)
        gifticons.value = currentGifticons.map { if (it.id == id) used else it }
        return AppResult.Success(used)
    }

    private fun earnedGifticon(sequence: Int): Gifticon =
        Gifticon(
            id = "stamp-reward-$sequence",
            title = "스탬프 완성 쿠폰",
            barcodeValue = "CAFE-MINSU-STAMP-$sequence",
            qrValue = "CAFE-MINSU-STAMP-QR-$sequence",
            expiresAtMillis = nowMillis() + REWARD_VALIDITY_MILLIS,
            status = GifticonStatus.Available,
        )

    private companion object {
        const val REWARD_VALIDITY_MILLIS = 1000L * 60L * 60L * 24L * 30L
    }
}
