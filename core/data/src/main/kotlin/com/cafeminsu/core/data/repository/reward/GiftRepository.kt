package com.cafeminsu.core.data.repository.reward

import com.cafeminsu.core.model.reward.GiftSendRequest
import com.cafeminsu.core.model.reward.GiftSendResult
import kotlinx.coroutines.flow.Flow

interface GiftRepository {
    fun sendGift(request: GiftSendRequest): Flow<GiftSendResult>
}
