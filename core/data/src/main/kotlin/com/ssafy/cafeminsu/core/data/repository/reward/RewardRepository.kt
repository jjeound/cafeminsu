package com.ssafy.cafeminsu.core.data.repository.reward

import com.ssafy.cafeminsu.core.model.reward.Gifticon
import com.ssafy.cafeminsu.core.model.reward.Stamp
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun getStamps(): Flow<List<Stamp>>

    fun getStamp(storeId: Long): Flow<Stamp>

    fun getGifticons(): Flow<List<Gifticon>>

    fun getGifticon(id: String): Flow<Gifticon>

    fun markGifticonUsed(id: String): Flow<Gifticon>
}
