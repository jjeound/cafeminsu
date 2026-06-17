package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockRewardRepositoryTest {
    @Test
    fun grantStampsForPaidOrderIncrementsCurrentCountAndHistory() = runBlocking {
        val repository = MockRewardRepository()
        val initial = MockData.initialStampCard

        val stampCard = repository.grantStampsForPaidOrder("order-1").successStampCard()

        assertEquals(initial.currentCount + 1, stampCard.currentCount)
        assertEquals(initial.history.size + 1, stampCard.history.size)
        assertEquals("order-1", stampCard.history.first().orderId)
    }

    @Test
    fun observeStampCardEmitsCurrentStampCard() = runBlocking {
        val repository = MockRewardRepository()

        repository.observeStampCard().test {
            assertEquals(MockData.initialStampCard, awaitItem().successData())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markGifticonUsedChangesStatusToUsed() = runBlocking {
        val repository = MockRewardRepository()
        val gifticon = MockData.initialGifticons.first { it.status == GifticonStatus.Available }

        val used = repository.markGifticonUsed(gifticon.id).successGifticon()

        assertEquals(GifticonStatus.Used, used.status)
        repository.observeGifticons().test {
            val gifticons = awaitItem().successData()
            assertEquals(GifticonStatus.Used, gifticons.first { it.id == gifticon.id }.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun AppResult<StampCard>.successStampCard(): StampCard {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<StampCard>).data
    }

    @Suppress("UNCHECKED_CAST")
    private fun AppResult<Gifticon>.successGifticon(): Gifticon {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<Gifticon>).data
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}
