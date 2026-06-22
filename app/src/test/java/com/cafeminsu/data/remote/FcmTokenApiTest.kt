package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class FcmTokenApiTest {
    @Test
    fun fcmTokenReqCarriesOpenApiField() {
        val request = FcmTokenReq(fcmToken = "device-token-abc123")

        assertEquals("device-token-abc123", request.fcmToken)
    }

    @Test
    fun fcmTokenResToleratesNullVoidResult() {
        val response = FcmTokenRes()

        assertEquals(null, response.ignored)
    }
}
