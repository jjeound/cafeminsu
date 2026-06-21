package com.cafeminsu.ui.feature.owner.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuAddUiStateTest {
    @Test
    fun defaultStateIsNotSubmittable() {
        val state = OwnerMenuAddUiState()

        assertEquals(OwnerMenuAddCategory.Coffee, state.category)
        assertTrue(state.onSale)
        assertNull(state.imageUri)
        assertFalse(state.canSubmit)
    }

    @Test
    fun requiresBothNameAndPositivePrice() {
        val nameOnly = OwnerMenuAddUiState(name = "아메리카노")
        val priceOnly = OwnerMenuAddUiState(priceInput = "4500")
        val zeroPrice = OwnerMenuAddUiState(name = "아메리카노", priceInput = "0")
        val valid = OwnerMenuAddUiState(name = "아메리카노", priceInput = "4500")

        assertFalse(nameOnly.canSubmit)
        assertFalse(priceOnly.canSubmit)
        assertFalse(zeroPrice.canSubmit)
        assertTrue(valid.canSubmit)
        assertEquals(4500, valid.price)
    }

    @Test
    fun blankNameIsNotValidAndIsTrimmed() {
        val blank = OwnerMenuAddUiState(name = "   ", priceInput = "4500")
        val padded = OwnerMenuAddUiState(name = "  라떼  ", priceInput = "4500")

        assertFalse(blank.canSubmit)
        assertEquals("라떼", padded.trimmedName)
        assertTrue(padded.canSubmit)
    }

    @Test
    fun submittingDisablesSubmit() {
        val state = OwnerMenuAddUiState(name = "아메리카노", priceInput = "4500", isSubmitting = true)

        assertFalse(state.canSubmit)
    }
}
