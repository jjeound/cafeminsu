package com.cafeminsu.data.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerDateTimeTest {
    @Test
    fun zonelessDateTimeIsParsedAsSeoulTime() {
        val expected = LocalDateTime.parse("2026-06-24T10:15:30")
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, parseServerEpochMillis("2026-06-24T10:15:30"))
    }

    @Test
    fun utcInstantFormIsParsed() {
        assertEquals(
            Instant.parse("2026-06-24T01:15:30Z").toEpochMilli(),
            parseServerEpochMillis("2026-06-24T01:15:30Z"),
        )
    }

    @Test
    fun offsetDateTimeFormIsParsed() {
        // +09:00 (KST) at 10:15:30 == 01:15:30Z, same instant as the UTC case.
        assertEquals(
            Instant.parse("2026-06-24T01:15:30Z").toEpochMilli(),
            parseServerEpochMillis("2026-06-24T10:15:30+09:00"),
        )
    }

    @Test
    fun nullValueIsZero() {
        assertEquals(0L, parseServerEpochMillis(null))
    }

    @Test
    fun blankValueIsZero() {
        assertEquals(0L, parseServerEpochMillis("   "))
    }

    @Test
    fun garbageValueIsZero() {
        assertEquals(0L, parseServerEpochMillis("not-a-date"))
    }
}
