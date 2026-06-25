package com.cafeminsu.domain.model

/**
 * 매장 NFC 태그로 발급받은 쿠폰 요약(발급 응답).
 *
 * 발급된 쿠폰은 별도 타입이 아니라 기존 기프티콘(전 매장 공용 금액형)이다. 본 모델은 발급 API 응답을
 * 화면 표시·성공 안내용으로 담고, 사용/결제는 기존 기프티콘 플로우를 재사용한다.
 */
data class NfcCoupon(
    val gifticonId: Long,
    val amount: Int,
    val expiresAtIso: String,
    val message: String?,
)
