package com.cafeminsu.ui.feature.gift.claim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftClaimUiStateTest {
    @Test
    fun canSubmitRequiresNonBlankCodeAndNotSubmitting() {
        assertTrue(GiftClaimUiState(code = "GFT-1234-5678").canSubmit)
    }

    @Test
    fun blankCodeCannotSubmit() {
        assertFalse(GiftClaimUiState(code = "   ").canSubmit)
        assertFalse(GiftClaimUiState(code = "").canSubmit)
    }

    @Test
    fun submittingDisablesSubmit() {
        assertFalse(GiftClaimUiState(code = "GFT-1234-5678", submitting = true).canSubmit)
    }
}
