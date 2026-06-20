package com.cafeminsu.data.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokenStoreTest {
    @Test
    fun exposesReadSaveUpdateAndClearContract() = runBlocking {
        val store = ContractSessionTokenStore()

        assertNull(store.read())

        store.save(SessionTokens("access", "refresh"))
        store.updateAccessToken("new-access")

        assertEquals(SessionTokens("new-access", "refresh"), store.read())

        store.clear()

        assertNull(store.read())
    }
}

private class ContractSessionTokenStore : SessionTokenStore {
    private var tokens: SessionTokens? = null

    override suspend fun read(): SessionTokens? = tokens

    override suspend fun save(tokens: SessionTokens) {
        this.tokens = tokens
    }

    override suspend fun updateAccessToken(accessToken: String) {
        tokens = tokens?.copy(accessToken = accessToken)
    }

    override suspend fun clear() {
        tokens = null
    }
}
