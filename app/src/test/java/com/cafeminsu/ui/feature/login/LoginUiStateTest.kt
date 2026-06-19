package com.cafeminsu.ui.feature.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LoginUiStateTest {
    @Test
    fun defaultStateIsIdle() {
        val state = LoginUiState()

        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertNull(state.errorMessage)
    }

    @Test
    fun navigateHomeEventHasStableType() {
        assertEquals(LoginEvent.NavigateHome, LoginEvent.NavigateHome)
    }
}
