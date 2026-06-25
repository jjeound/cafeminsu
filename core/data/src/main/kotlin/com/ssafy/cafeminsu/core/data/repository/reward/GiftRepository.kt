package com.ssafy.cafeminsu.core.data.repository.reward

import com.ssafy.cafeminsu.core.model.reward.GiftSendRequest
import com.ssafy.cafeminsu.core.model.reward.GiftSendResult
import kotlinx.coroutines.flow.Flow

interface GiftRepository {
    fun sendGift(request: GiftSendRequest): Flow<GiftSendResult>
}
