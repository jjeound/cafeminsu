package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.StampCard
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observeStampCard(): Flow<AppResult<StampCard>>
    suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard>
    fun observeGifticons(): Flow<AppResult<List<Gifticon>>>
    suspend fun getGifticon(id: String): AppResult<Gifticon>
    suspend fun markGifticonUsed(id: String): AppResult<Gifticon>
}
