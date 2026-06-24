package com.cafeminsu.ui.feature.gift.claim

import java.net.URI
import java.net.URLDecoder

/**
 * 선물 등록(claim) 딥링크/코드 입력 검증 유틸.
 *
 * 보안(SECURITY §6): 허용된 scheme/host(화이트리스트)만 수락하고, claim 코드의 문자/길이를 경계 검증한다.
 * claimCode 는 베어러 토큰처럼 동작하므로 값 자체는 로깅하지 않는다(KAKAO_GIFT_BACKEND §1).
 */
object GiftClaimDeepLink {
    const val SCHEME = "cafeminsu"
    const val HOST = "gift"
    const val CODE_PARAM = "code"

    /** 딥링크 패턴: [Routes 의 GIFT_CLAIM 딥링크와 1:1]. */
    const val URI_PATTERN = "$SCHEME://$HOST?$CODE_PARAM={$CODE_PARAM}"

    const val MAX_CODE_LENGTH = 64
    private const val MIN_CODE_LENGTH = 4
    private val codeRegex = Regex("^[A-Za-z0-9-]+$")

    /**
     * 허용된 scheme/host 딥링크에서만 code 쿼리를 추출·검증해 반환한다. 그 외(타 scheme/host·형식 위반)는 null.
     */
    fun extractClaimCode(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        val parsed = runCatching { URI(uri.trim()) }.getOrNull() ?: return null
        if (!SCHEME.equals(parsed.scheme, ignoreCase = true)) return null
        val host = parsed.host ?: parsed.authority
        if (!HOST.equals(host, ignoreCase = true)) return null
        val rawQuery = parsed.rawQuery ?: return null
        val code = rawQuery.findParam(CODE_PARAM) ?: return null
        return code.takeIf(::isValidClaimCode)
    }

    /** claim 코드 형식/길이 검증(허용 문자: 영문·숫자·하이픈). */
    fun isValidClaimCode(code: String): Boolean {
        val trimmed = code.trim()
        return trimmed.length in MIN_CODE_LENGTH..MAX_CODE_LENGTH && codeRegex.matches(trimmed)
    }

    /** 딥링크 인자 등 신뢰할 수 없는 입력을 정규화. 유효하지 않으면 빈 문자열. */
    fun normalizeCode(raw: String?): String =
        raw?.trim()?.takeIf(::isValidClaimCode).orEmpty()

    /** 수동 입력 정규화: 허용 문자만 남기고 최대 길이로 제한(과대 입력 차단). */
    fun sanitizeInput(raw: String): String =
        raw.filter { it.isLetterOrDigit() || it == '-' }.take(MAX_CODE_LENGTH)

    private fun String.findParam(name: String): String? =
        split("&").firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) {
                return@firstNotNullOfOrNull null
            }
            val key = part.substring(0, separator)
            if (key != name) {
                null
            } else {
                runCatching { URLDecoder.decode(part.substring(separator + 1), "UTF-8") }.getOrNull()
            }
        }
}
