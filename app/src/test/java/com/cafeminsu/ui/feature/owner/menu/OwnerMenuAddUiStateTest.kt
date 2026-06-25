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

    @Test
    fun noOptionGroupsIsStillSubmittable() {
        val state = OwnerMenuAddUiState(name = "아메리카노", priceInput = "4500")

        assertTrue(state.optionGroups.isEmpty())
        assertTrue(state.canSubmit)
    }

    @Test
    fun completeOptionGroupAllowsSubmit() {
        val state = OwnerMenuAddUiState(
            name = "아메리카노",
            priceInput = "4500",
            optionGroups = listOf(
                OwnerMenuOptionGroupInput(
                    id = "g1",
                    name = "사이즈",
                    options = listOf(
                        OwnerMenuOptionInput(id = "o1", name = "Tall", priceInput = ""),
                        OwnerMenuOptionInput(id = "o2", name = "Grande", priceInput = "500"),
                    ),
                ),
            ),
        )

        assertTrue(state.canSubmit)
        assertEquals(0, state.optionGroups.first().options.first().extraPrice)
        assertEquals(500, state.optionGroups.first().options[1].extraPrice)
    }

    @Test
    fun incompleteOptionGroupBlocksSubmit() {
        val base = OwnerMenuAddUiState(name = "아메리카노", priceInput = "4500")
        val blankGroupName = base.copy(
            optionGroups = listOf(
                OwnerMenuOptionGroupInput(id = "g1", name = "  ", options = listOf(OwnerMenuOptionInput(id = "o1", name = "Tall"))),
            ),
        )
        val noOptions = base.copy(
            optionGroups = listOf(OwnerMenuOptionGroupInput(id = "g1", name = "사이즈", options = emptyList())),
        )
        val blankOptionName = base.copy(
            optionGroups = listOf(
                OwnerMenuOptionGroupInput(id = "g1", name = "사이즈", options = listOf(OwnerMenuOptionInput(id = "o1", name = "   "))),
            ),
        )

        assertFalse(blankGroupName.canSubmit)
        assertFalse(noOptions.canSubmit)
        assertFalse(blankOptionName.canSubmit)
    }
}
