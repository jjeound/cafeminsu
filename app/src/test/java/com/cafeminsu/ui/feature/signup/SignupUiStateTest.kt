package com.cafeminsu.ui.feature.signup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupUiStateTest {
    @Test
    fun defaultStateCannotSubmit() {
        val state = SignupUiState()

        assertEquals(0, state.charCount)
        assertFalse(state.isNicknameValid)
        assertFalse(state.canSubmit)
    }

    @Test
    fun validNicknameCanSubmit() {
        val state = SignupUiState(nickname = "민수")

        assertEquals(2, state.charCount)
        assertTrue(state.isNicknameValid)
        assertTrue(state.canSubmit)
    }

    @Test
    fun tenCharNicknameIsValid() {
        val state = SignupUiState(nickname = "가나다라마바사아자차")

        assertEquals(10, state.charCount)
        assertTrue(state.isNicknameValid)
    }

    @Test
    fun shortNicknameIsInvalid() {
        assertFalse(SignupUiState(nickname = "민").isNicknameValid)
    }

    @Test
    fun invalidCharacterIsInvalid() {
        assertFalse(SignupUiState(nickname = "민수!").isNicknameValid)
        assertFalse(SignupUiState(nickname = "민 수").isNicknameValid)
    }

    @Test
    fun alphanumericNicknameIsValid() {
        assertTrue(SignupUiState(nickname = "cafe7").isNicknameValid)
    }

    @Test
    fun loadingBlocksSubmitEvenWhenValid() {
        val state = SignupUiState(nickname = "민수", isLoading = true)

        assertTrue(state.isNicknameValid)
        assertFalse(state.canSubmit)
    }
}
