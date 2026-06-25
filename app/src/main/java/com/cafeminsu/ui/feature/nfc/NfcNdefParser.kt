package com.cafeminsu.ui.feature.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * NDEF 메시지/레코드에서 발급용 raw 문자열을 추출하는 안드로이드 글루.
 *
 * 추출된 raw 는 도메인 파서([com.cafeminsu.domain.nfc.NfcTagCode])로 코드화한다(레이어 분리).
 * 깨진 태그/미지원 레코드는 예외를 전파하지 않고 null 로 흡수한다(크래시 금지). raw 값은 로깅하지 않는다.
 */
object NfcNdefParser {
    /** 메시지의 레코드를 순회하며 첫 유효 문자열을 반환한다(없으면 null). */
    fun extractRawCode(message: NdefMessage?): String? {
        val records = message?.records ?: return null
        for (record in records) {
            val value = runCatching { recordToString(record) }.getOrNull()
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return null
    }

    private fun recordToString(record: NdefRecord): String? {
        val isTextRecord = record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
        if (isTextRecord) {
            return decodeTextRecordPayload(record.payload)
        }
        // URI well-known/absolute 레코드는 toUri() 가 안전하게 변환한다(아니면 null).
        return runCatching { record.toUri()?.toString() }.getOrNull()
    }

    /**
     * RTD_TEXT 레코드 페이로드(상태 바이트 + 언어코드 + 본문) → 본문 문자열. 순수 바이트 로직(테스트 대상).
     * 상태 바이트 bit7 = 인코딩(0=UTF-8/1=UTF-16), 하위 6비트 = 언어코드 길이.
     * 비거나 길이가 어긋나거나 디코딩 실패/공백이면 null(예외 전파 없음).
     */
    fun decodeTextRecordPayload(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val status = payload[0].toInt()
        val isUtf16 = (status and TEXT_ENCODING_MASK) != 0
        val languageLength = status and TEXT_LANGUAGE_LENGTH_MASK
        val textOffset = 1 + languageLength
        if (payload.size < textOffset) return null

        val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        return runCatching {
            String(payload, textOffset, payload.size - textOffset, charset)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private const val TEXT_ENCODING_MASK = 0x80
    private const val TEXT_LANGUAGE_LENGTH_MASK = 0x3F
}
