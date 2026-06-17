package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.StampCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class RewardRepositoryTest {
    @Test
    fun exposesRewardRepositoryContract() = runBlocking {
        val repository = object : RewardRepository {
            override fun observeStampCard(): Flow<AppResult<StampCard>> =
                flowOf(AppResult.Failure(com.cafeminsu.core.DomainError.Unknown))

            override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
                flowOf(AppResult.Success(emptyList()))

            override suspend fun getGifticon(id: String): AppResult<Gifticon> =
                AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

            override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)
        }

        val gifticons: Flow<AppResult<List<Gifticon>>> = repository.observeGifticons()
        assertNotNull(gifticons)
    }
}
