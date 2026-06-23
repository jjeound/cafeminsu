package com.cafeminsu.core.data.repository.reward

import com.cafeminsu.core.model.reward.Gifticon
import com.cafeminsu.core.model.reward.Stamp
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observeStamps(): Flow<List<Stamp>>

    fun observeStamp(storeId: Long): Flow<Stamp>

    fun observeGifticons(): Flow<List<Gifticon>>

    fun getGifticon(id: String): Flow<Gifticon>

    fun markGifticonUsed(id: String): Flow<Gifticon>
}
