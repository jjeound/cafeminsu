package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.NfcCoupon
import com.cafeminsu.domain.repository.NfcCouponRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockNfcCouponRepository : NfcCouponRepository {
    @Inject
    constructor()

    private var nextGifticonSequence = 1L

    override suspend fun claim(tagCode: String): AppResult<NfcCoupon> {
        val normalizedTagCode = tagCode.trim()
        if (normalizedTagCode.isBlank()) {
            return AppResult.Failure(DomainError.Validation("tagCode"))
        }

        // 알아보기 쉬운 규칙으로 실패도 시뮬레이션한다(개발/테스트용).
        return when {
            normalizedTagCode.contains(CooldownMarker, ignoreCase = true) ->
                AppResult.Failure(DomainError.Payment("nfc-cooldown"))

            normalizedTagCode.contains(NotFoundMarker, ignoreCase = true) ->
                AppResult.Failure(DomainError.NotFound)

            normalizedTagCode.contains(InactiveMarker, ignoreCase = true) ->
                AppResult.Failure(DomainError.Payment("nfc-inactive"))

            else -> AppResult.Success(
                NfcCoupon(
                    gifticonId = nextGifticonSequence++,
                    amount = DefaultAmount,
                    expiresAtIso = DefaultExpiresAtIso,
                    message = DefaultMessage,
                ),
            )
        }
    }

    private companion object {
        const val CooldownMarker = "COOLDOWN"
        const val NotFoundMarker = "NOTFOUND"
        const val InactiveMarker = "INACTIVE"
        const val DefaultAmount = 1_000
        const val DefaultExpiresAtIso = "2026-12-25T10:30:00"
        const val DefaultMessage = "방문 감사 쿠폰"
    }
}
