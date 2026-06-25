package com.cafeminsu.ui.feature.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NfcNdefParserTest {
    @Test
    fun `decodes UTF-8 text record payload skipping language code`() {
        // status byte: UTF-8(0x00..), lang length 2 ("en")
        val payload = byteArrayOf(0x02) + "en".toByteArray(Charsets.UTF_8) +
            "GFT-1234-5678".toByteArray(Charsets.UTF_8)

        assertEquals("GFT-1234-5678", NfcNdefParser.decodeTextRecordPayload(payload))
    }

    @Test
    fun `decodes UTF-16 text record payload when status high bit set`() {
        // status byte 0x82 → UTF-16, lang length 2
        val text = "GFT-ABCD"
        val payload = byteArrayOf(0x82.toByte()) + "ko".toByteArray(Charsets.UTF_8) +
            text.toByteArray(Charsets.UTF_16)

        assertEquals(text, NfcNdefParser.decodeTextRecordPayload(payload))
    }

    @Test
    fun `returns null for empty or malformed text payload`() {
        assertNull(NfcNdefParser.decodeTextRecordPayload(byteArrayOf()))
        // lang length 10 but payload too short → null (no crash)
        assertNull(NfcNdefParser.decodeTextRecordPayload(byteArrayOf(0x0A)))
    }

    @Test
    fun `returns null when decoded text is blank`() {
        val payload = byteArrayOf(0x02) + "en".toByteArray(Charsets.UTF_8)

        assertNull(NfcNdefParser.decodeTextRecordPayload(payload))
    }
}
