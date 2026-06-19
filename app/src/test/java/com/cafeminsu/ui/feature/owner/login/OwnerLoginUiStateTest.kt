package com.cafeminsu.ui.feature.owner.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerLoginUiStateTest {
    @Test
    fun defaultStateDoesNotExposePasswordOrAuthenticatedStatus() {
        val state = OwnerLoginUiState()

        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertEquals(null, state.errorMessage)
        assertFalse(state.toString().contains("password", ignoreCase = true))
    }

    @Test
    fun ownerLoginEventsCarryNavigationOrSafeSnackbarMessageOnly() {
        val navigateEvent = OwnerLoginEvent.NavigateOwnerHome
        val snackbarEvent = OwnerLoginEvent.ShowSnackbar("아이디 또는 비밀번호를 확인해 주세요")

        assertEquals(OwnerLoginEvent.NavigateOwnerHome, navigateEvent)
        assertTrue(snackbarEvent.message.contains("확인"))
        assertFalse(snackbarEvent.toString().contains("owner-secret"))
    }
}
