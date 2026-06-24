package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.toGifticon
import com.cafeminsu.data.mapper.toGiftSendResult
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.GifticonClaimReq
import com.cafeminsu.data.remote.GifticonClaimRes
import com.cafeminsu.data.remote.GifticonPurchaseReq
import com.cafeminsu.data.remote.GifticonPurchaseRes
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toDomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.repository.GiftRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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

    override suspend fun claimGift(claimCode: String): AppResult<Gifticon> =
        withContext(ioDispatcher) {
            val normalizedCode = claimCode.trim()
            if (normalizedCode.isBlank()) {
                return@withContext AppResult.Failure(DomainError.Validation("claimCode"))
            }

            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            when (val response = claimGifticon(normalizedCode)) {
                is AppResult.Success -> response.data.toGifticon()
                is AppResult.Failure -> response
            }
        }

    private suspend fun claimGifticon(claimCode: String): AppResult<GifticonClaimRes> =
        try {
            AppResult.Success(gifticonApi.claimGifticon(GifticonClaimReq(claimCode = claimCode)))
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            AppResult.Failure(throwable.toClaimDomainError())
        }

    // 이미 등록된 코드(409)는 별도 도메인 에러로 구분. 그 외는 공통 매핑.
    private fun Throwable.toClaimDomainError(): DomainError =
        if (this is HttpException && code() == HttpConflict) {
            DomainError.Payment("already-claimed")
        } else {
            toDomainError()
        }

    private suspend fun purchaseGifticon(request: GiftSendRequest): AppResult<GifticonPurchaseRes> =
        runCatchingToAppResult {
            gifticonApi.purchaseGifticon(request = request.toPurchaseReq())
        }

    private fun GiftSendRequest.toPurchaseReq(): GifticonPurchaseReq =
        when (channel) {
            // 친구 선물: 수신자 미지정 구매. 친구 uuid 는 서버로 보내지 않고(발신자만 JWT 로 식별)
            // 메시지 전송 전용으로 UI 레이어에서만 사용한다(docs/KAKAO_GIFT_BACKEND.md §1).
            GiftChannel.KakaoTalk -> GifticonPurchaseReq(
                amount = amount,
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
            // KakaoTalk 은 선택된 친구(uuid) 존재만 전제(빈 선택 차단). 숫자 receiverId 를 더는 요구하지 않는다.
            request.recipientRef.isBlank() -> DomainError.Validation("recipient")
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
        const val HttpConflict = 409
    }
}
