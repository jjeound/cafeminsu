package com.cafeminsu.ui.feature.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

class NfcReaderEffectTest {
    @Test
    fun `no adapter maps to unsupported`() {
        assertEquals(
            NfcAvailability.Unsupported,
            resolveNfcAvailability(hasAdapter = false, enabled = false),
        )
        assertEquals(
            NfcAvailability.Unsupported,
            resolveNfcAvailability(hasAdapter = false, enabled = true),
        )
    }

    @Test
    fun `adapter present but disabled maps to disabled`() {
        assertEquals(
            NfcAvailability.Disabled,
            resolveNfcAvailability(hasAdapter = true, enabled = false),
        )
    }

    @Test
    fun `adapter present and enabled maps to ready`() {
        assertEquals(
            NfcAvailability.Ready,
            resolveNfcAvailability(hasAdapter = true, enabled = true),
        )
    }
}
