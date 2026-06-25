package com.cafeminsu.data.mapper

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * 서버의 `format: date-time` 문자열을 epoch millis 로 변환한다.
 *
 * 서버(Spring)는 다음 형태를 섞어 보낼 수 있어 순서대로 시도한다:
 *  1. UTC Instant (`2026-06-24T01:15:30Z`)
 *  2. 오프셋 포함 (`2026-06-24T10:15:30+09:00`)
 *  3. 존 없는 로컬 시각 (`2026-06-24T10:15:30`) — 한국 시간(Asia/Seoul)으로 해석
 *
 * null/blank/파싱 불가 값은 [UnparsableEpochMillis] 로 폴백한다(예외 전파 금지).
 */
fun parseServerEpochMillis(value: String?): Long {
    if (value.isNullOrBlank()) {
        return UnparsableEpochMillis
    }

    try {
        return Instant.parse(value).toEpochMilli()
    } catch (_: DateTimeParseException) {
        // 다음 형식 시도
    }

    try {
        return OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        // 다음 형식 시도
    }

    return try {
        LocalDateTime.parse(value)
            .atZone(KoreaZone)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        UnparsableEpochMillis
    } catch (_: DateTimeException) {
        UnparsableEpochMillis
    }
}

private val KoreaZone: ZoneId = ZoneId.of("Asia/Seoul")

private const val UnparsableEpochMillis = 0L
