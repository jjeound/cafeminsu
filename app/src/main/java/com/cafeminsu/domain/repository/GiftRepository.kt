package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult

interface GiftRepository {
    suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult>
}
