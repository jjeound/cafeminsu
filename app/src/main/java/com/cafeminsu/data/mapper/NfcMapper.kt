package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.NfcClaimRes
import com.cafeminsu.domain.model.NfcCoupon

/**
 * 발급 응답 → 도메인 모델 매핑. 필수 필드(gifticonId/amount/expiresAt)가 비면 발급 실패로 본다.
 * 민감값은 없으나 응답을 통째로 로깅하지 않는다.
 */
fun NfcClaimRes.toNfcCoupon(): AppResult<NfcCoupon> {
    val id = gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    val resolvedAmount = amount ?: return AppResult.Failure(DomainError.Unknown)
    val resolvedExpiresAt = expiresAt?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        NfcCoupon(
            gifticonId = id,
            amount = resolvedAmount,
            expiresAtIso = resolvedExpiresAt,
            message = message,
        ),
    )
}
