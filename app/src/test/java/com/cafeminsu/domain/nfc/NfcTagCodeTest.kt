package com.cafeminsu.domain.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NfcTagCodeTest {
    @Test
    fun parsePlainCodeReturnsTrimmedCode() {
        assertEquals("NFC-AB7K-9QM2", NfcTagCode.parse("NFC-AB7K-9QM2"))
    }

    @Test
    fun parseTrimsSurroundingWhitespace() {
        assertEquals("NFC-AB7K-9QM2", NfcTagCode.parse("  NFC-AB7K-9QM2  "))
    }

    @Test
    fun parsePreservesCase() {
        assertEquals("nfc-ab7k", NfcTagCode.parse("nfc-ab7k"))
    }

    @Test
    fun parseHttpsUrlExtractsCodeQueryParam() {
        assertEquals(
            "NFC-AB7K-9QM2",
            NfcTagCode.parse("https://cafeminsu.example/nfc?code=NFC-AB7K-9QM2"),
        )
    }

    @Test
    fun parseHttpUrlExtractsCodeQueryParam() {
        assertEquals(
            "NFC-AB7K-9QM2",
            NfcTagCode.parse("http://cafeminsu.example/nfc?code=NFC-AB7K-9QM2"),
        )
    }

    @Test
    fun parseUrlWithMultipleQueryParamsExtractsCode() {
        assertEquals(
            "ABC-123",
            NfcTagCode.parse("https://x/nfc?store=1&code=ABC-123&n=2"),
        )
    }

    @Test
    fun parseCustomSchemeUrlExtractsCode() {
        assertEquals("ABC-123", NfcTagCode.parse("cafeminsu://nfc?code=ABC-123"))
    }

    @Test
    fun parseUrlWithoutCodeParamReturnsNull() {
        assertNull(NfcTagCode.parse("https://cafeminsu.example/nfc?store=1"))
    }

    @Test
    fun parseUrlWithEmptyCodeParamReturnsNull() {
        assertNull(NfcTagCode.parse("https://cafeminsu.example/nfc?code="))
    }

    @Test
    fun parseUrlWithCodeContainingIllegalCharReturnsNull() {
        // 디코딩 후 공백(허용 문자 위반)
        assertNull(NfcTagCode.parse("https://x/nfc?code=AB%20CD"))
    }

    @Test
    fun parseNullReturnsNull() {
        assertNull(NfcTagCode.parse(null))
    }

    @Test
    fun parseBlankReturnsNull() {
        assertNull(NfcTagCode.parse("   "))
    }

    @Test
    fun parseIllegalCharactersReturnsNull() {
        assertNull(NfcTagCode.parse("NFC_AB7K!9QM2"))
    }

    @Test
    fun parseOverLongCodeReturnsNull() {
        val tooLong = "A".repeat(NfcTagCode.MAX_LENGTH + 1)
        assertNull(NfcTagCode.parse(tooLong))
    }

    @Test
    fun parseMaxLengthCodeIsAccepted() {
        val maxLength = "A".repeat(NfcTagCode.MAX_LENGTH)
        assertEquals(maxLength, NfcTagCode.parse(maxLength))
    }
}
