package com.cafeminsu.data.auth

import com.cafeminsu.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStateHolderTest {
    @Test
    fun startsAsUnknownAndUpdatesAuthState() {
        val holder = SessionStateHolder()

        assertEquals(AuthState.Unknown, holder.authState.value)

        holder.update(AuthState.Expired)

        assertEquals(AuthState.Expired, holder.authState.value)
    }
}
