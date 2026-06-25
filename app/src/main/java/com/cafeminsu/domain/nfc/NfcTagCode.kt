package com.cafeminsu.domain.nfc

import java.net.URI
import java.net.URLDecoder

/**
 * NDEF 레코드에서 추출한 raw 문자열 → 발급용 코드 변환 순수 유틸.
 *
 * 안드로이드 비종속(NDEF/Context 미주입). NFC 읽기는 상위 레이어(step2~3)에서 수행하고, 본 유틸은
 * 추출된 문자열만 다룬다. 보안(SECURITY §6): scheme 검증 없이도 코드 문자/길이를 경계 검증한다.
 */
object NfcTagCode {
    const val MAX_LENGTH = 64
    private const val MIN_LENGTH = 1
    private const val CODE_PARAM = "code"
    private val codeRegex = Regex("^[A-Za-z0-9-]+$")

    /**
     * raw 문자열에서 발급용 코드를 추출·검증해 반환한다. 추출 불가/형식 위반/과대 길이면 null.
     *
     * - null/공백 → null
     * - URL(`://` 포함) → 쿼리파라미터 `code` 값만 추출(디코딩), 없으면 null
     * - 순수 코드 → trim 후 그대로
     */
    fun parse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val candidate = if (trimmed.looksLikeUrl()) {
            trimmed.extractCodeParam() ?: return null
        } else {
            trimmed
        }
        return candidate.takeIf(::isValid)
    }

    private fun String.looksLikeUrl(): Boolean = contains("://")

    private fun String.extractCodeParam(): String? {
        val uri = runCatching { URI(this) }.getOrNull() ?: return null
        val rawQuery = uri.rawQuery ?: return null
        return rawQuery.split("&").firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) {
                return@firstNotNullOfOrNull null
            }
            val key = part.substring(0, separator)
            if (key != CODE_PARAM) {
                null
            } else {
                runCatching { URLDecoder.decode(part.substring(separator + 1), "UTF-8") }.getOrNull()
            }
        }
    }

    private fun isValid(code: String): Boolean =
        code.length in MIN_LENGTH..MAX_LENGTH && codeRegex.matches(code)
}
