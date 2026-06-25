package com.cafeminsu.ui.feature.gift.claim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftClaimDeepLinkTest {
    @Test
    fun extractsCodeFromAllowedSchemeAndHost() {
        val code = GiftClaimDeepLink.extractClaimCode("cafeminsu://gift?code=GFT-1234-5678")

        assertEquals("GFT-1234-5678", code)
    }

    @Test
    fun rejectsForeignScheme() {
        assertNull(GiftClaimDeepLink.extractClaimCode("https://gift?code=GFT-1234-5678"))
        assertNull(GiftClaimDeepLink.extractClaimCode("evil://gift?code=GFT-1234-5678"))
    }

    @Test
    fun rejectsForeignHost() {
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://evil?code=GFT-1234-5678"))
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://order?code=GFT-1234-5678"))
    }

    @Test
    fun rejectsMissingOrMalformedCode() {
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://gift"))
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://gift?code="))
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://gift?code=한글코드"))
        assertNull(GiftClaimDeepLink.extractClaimCode("cafeminsu://gift?code=ab"))
        assertNull(GiftClaimDeepLink.extractClaimCode(null))
        assertNull(GiftClaimDeepLink.extractClaimCode("   "))
    }

    @Test
    fun validatesCodeFormatAndLength() {
        assertTrue(GiftClaimDeepLink.isValidClaimCode("GFT-1234-5678"))
        assertTrue(GiftClaimDeepLink.isValidClaimCode("ABCD1234"))
        assertFalse(GiftClaimDeepLink.isValidClaimCode(""))
        assertFalse(GiftClaimDeepLink.isValidClaimCode("ab"))
        assertFalse(GiftClaimDeepLink.isValidClaimCode("코드"))
        assertFalse(GiftClaimDeepLink.isValidClaimCode("CODE WITH SPACE"))
        assertFalse(GiftClaimDeepLink.isValidClaimCode("X".repeat(100)))
    }

    @Test
    fun normalizeKeepsValidCodeAndDropsInvalid() {
        assertEquals("GFT-1234-5678", GiftClaimDeepLink.normalizeCode("  GFT-1234-5678 "))
        assertEquals("", GiftClaimDeepLink.normalizeCode("ab"))
        assertEquals("", GiftClaimDeepLink.normalizeCode(null))
        assertEquals("", GiftClaimDeepLink.normalizeCode("코드"))
    }

    @Test
    fun buildClaimUriRoundTripsWithExtractClaimCode() {
        val uri = GiftClaimDeepLink.buildClaimUri("GFT-1234-5678")

        assertEquals("cafeminsu://gift?code=GFT-1234-5678", uri)
        assertEquals("GFT-1234-5678", GiftClaimDeepLink.extractClaimCode(uri))
    }

    @Test
    fun sanitizeInputDropsDisallowedCharactersAndCaps() {
        assertEquals("GFT1234", GiftClaimDeepLink.sanitizeInput("GFT 1234!@#"))
        assertEquals("GFT-1234", GiftClaimDeepLink.sanitizeInput("GFT-1234!@#"))
        assertEquals(GiftClaimDeepLink.MAX_CODE_LENGTH, GiftClaimDeepLink.sanitizeInput("A".repeat(200)).length)
    }
}
