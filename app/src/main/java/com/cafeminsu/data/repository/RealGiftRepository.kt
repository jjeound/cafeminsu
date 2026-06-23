package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.toGiftSendResult
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.GifticonPurchaseReq
import com.cafeminsu.data.remote.GifticonPurchaseRes
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.repository.GiftRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class RealGiftRepository(
    private val gifticonApi: GifticonApi,
    private val sessionStateHolder: SessionStateHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nowMillis: () -> Long,
) : GiftRepository {
    @Inject
    constructor(
        gifticonApi: GifticonApi,
        sessionStateHolder: SessionStateHolder,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        gifticonApi = gifticonApi,
        sessionStateHolder = sessionStateHolder,
        ioDispatcher = ioDispatcher,
        nowMillis = { System.currentTimeMillis() },
    )

    override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> =
        withContext(ioDispatcher) {
            validate(request)?.let { error -> return@withContext AppResult.Failure(error) }

            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            val purchase = when (val result = purchaseGifticon(request)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }
            val gifticonId = purchase.gifticonId
                ?: return@withContext AppResult.Failure(DomainError.Unknown)

            when (
                val response = runCatchingToAppResult {
                    gifticonApi.shareGifticon(gifticonId = gifticonId)
                }
            ) {
                is AppResult.Success -> response.data.toGiftSendResult(
                    purchase = purchase,
                    sentAtMillis = nowMillis(),
                )

                is AppResult.Failure -> response
            }
        }

    private suspend fun purchaseGifticon(request: GiftSendRequest): AppResult<GifticonPurchaseRes> =
        runCatchingToAppResult {
            gifticonApi.purchaseGifticon(request = request.toPurchaseReq())
        }

    private fun GiftSendRequest.toPurchaseReq(): GifticonPurchaseReq =
        when (channel) {
            GiftChannel.KakaoTalk -> GifticonPurchaseReq(
                amount = amount,
                receiverId = recipientRef.trim().toLong(),
                message = message.normalizedMessage(),
            )

            GiftChannel.Sms -> GifticonPurchaseReq(
                amount = amount,
                receiverPhone = recipientRef.trim(),
                message = message.normalizedMessage(),
            )
        }

    private fun validate(request: GiftSendRequest): DomainError? =
        when {
            request.amount < MinimumGiftAmount -> DomainError.Validation("amount")
            request.recipientRef.isBlank() -> DomainError.Validation("recipient")
            request.channel == GiftChannel.KakaoTalk && request.recipientRef.trim().toLongOrNull() == null ->
                DomainError.Validation("recipient")

            request.message != null && request.message.length > MaxMessageLength ->
                DomainError.Validation("message")

            else -> null
        }

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }

    private fun String?.normalizedMessage(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val MinimumGiftAmount = 1_000
        const val MaxMessageLength = 200
    }
}
