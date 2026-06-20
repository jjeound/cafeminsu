package com.cafeminsu.data.auth

import org.junit.Assert.assertFalse
import org.junit.Test

class EncryptedSessionTokenStoreTest {
    @Test
    fun sessionTokensDoNotExposeRawValuesInToString() {
        val tokens = SessionTokens(
            accessToken = "access-secret",
            refreshToken = "refresh-secret",
        )

        assertFalse(tokens.toString().contains("access-secret"))
        assertFalse(tokens.toString().contains("refresh-secret"))
    }
}
