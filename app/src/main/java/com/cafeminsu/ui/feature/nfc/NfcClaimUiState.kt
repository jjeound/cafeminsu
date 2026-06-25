package com.cafeminsu.ui.feature.nfc

import com.cafeminsu.domain.model.NfcCoupon
import java.text.NumberFormat
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * 매장 NFC 태그 쿠폰 발급 화면 상태. 발급 호출 진행중(claiming) 가드 + 성공 결과 요약 + 인라인 에러(한국어).
 * 성공 시점은 일회성 [NfcClaimEvent.Claimed] 로 화면에 전달한다(낙관 금지 — 서버 성공 응답 이후만).
 */
data class NfcClaimUiState(
    val claiming: Boolean = false,
    val claimedCoupon: NfcClaimResultUi? = null,
    val errorMessage: String? = null,
)

/** 발급 성공 결과 요약(표시용). 민감 식별자(gifticonId 등)는 담지 않는다. */
data class NfcClaimResultUi(
    val amountLabel: String,
    val expiresLabel: String,
    val message: String?,
)

sealed interface NfcClaimEvent {
    /** 발급 성공 — 안내(토스트/다이얼로그) 후 기프티콘 목록으로 이동. 민감값은 싣지 않는다. */
    data class Claimed(val coupon: NfcClaimResultUi) : NfcClaimEvent
}

/**
 * 발급 응답(도메인) → 화면 표시용 요약. 금액/유효기한 라벨을 한국어로 포맷한다.
 * ISO 파싱 실패는 graceful — 유효기한은 원문 문자열로 폴백(예외 전파 없음).
 */
fun NfcCoupon.toResultUi(): NfcClaimResultUi =
    NfcClaimResultUi(
        amountLabel = formatAmountLabel(amount),
        expiresLabel = formatExpiresLabel(expiresAtIso),
        message = message?.takeIf { it.isNotBlank() },
    )

private fun formatAmountLabel(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private fun formatExpiresLabel(iso: String): String {
    val date = parseExpiryDate(iso) ?: return iso
    return "${date.format(ExpiryDateFormatter)} 까지"
}

private fun parseExpiryDate(value: String): LocalDate? {
    if (value.isBlank()) return null

    runCatching { return Instant.parse(value).atZone(KoreaZone).toLocalDate() }
        .onFailure { it.rethrowIfUnexpected() }
    runCatching { return OffsetDateTime.parse(value).toLocalDate() }
        .onFailure { it.rethrowIfUnexpected() }
    runCatching { return LocalDateTime.parse(value).toLocalDate() }
        .onFailure { it.rethrowIfUnexpected() }
    runCatching { return LocalDate.parse(value) }
        .onFailure { it.rethrowIfUnexpected() }

    return null
}

private fun Throwable.rethrowIfUnexpected() {
    if (this !is DateTimeParseException && this !is DateTimeException) {
        throw this
    }
}

private val KoreaZone: ZoneId = ZoneId.of("Asia/Seoul")
private val ExpiryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
