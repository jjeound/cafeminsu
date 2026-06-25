package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.repository.GiftRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGiftRepository(
    private val nowMillis: () -> Long,
) : GiftRepository {
    @Inject
    constructor() : this(nowMillis = { System.currentTimeMillis() })

    private var nextGiftSequence = 1

    override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> {
        val validationError = validate(request)
        if (validationError != null) {
            return AppResult.Failure(validationError)
        }

        val sequence = nextGiftSequence++
        val result = GiftSendResult(
            giftId = "gift-$sequence",
            sentAtMillis = nowMillis(),
            // 개발/Mock 빌드에서 인텐트 공유가 동작하도록 샘플 클레임 코드를 제공한다.
            claimCode = "GFT-MOCK-%04d".format(sequence),
        )
        return AppResult.Success(result)
    }

    override suspend fun claimGift(claimCode: String): AppResult<Gifticon> {
        val normalizedCode = claimCode.trim()
        if (normalizedCode.isBlank()) {
            return AppResult.Failure(DomainError.Validation("claimCode"))
        }

        val gifticon = Gifticon(
            id = "gifticon-${nextGiftSequence++}",
            title = "금액형 기프티콘",
            barcodeValue = "mock-barcode",
            qrValue = "mock-qr",
            expiresAtMillis = nowMillis() + ThirtyDaysMillis,
            status = GifticonStatus.Available,
        )
        return AppResult.Success(gifticon)
    }

    private fun validate(request: GiftSendRequest): DomainError? =
        when {
            request.amount <= 0 -> DomainError.Validation("amount")
            request.message != null && request.message.length > MaxMessageLength ->
                DomainError.Validation("message")

            else -> null
        }

    private companion object {
        const val MaxMessageLength = 100
        const val ThirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
    }
}
