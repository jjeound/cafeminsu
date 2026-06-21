package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonPurchaseRes
import com.cafeminsu.data.remote.GifticonShareRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftMapperTest {
    @Test
    fun shareResponseMapsPurchaseIdAndSentAtWithoutExposingLinks() {
        val result = GifticonShareRes(
            shareLink = "https://cafeminsu.example/gift/secret",
            deepLink = "cafeminsu://gift/secret",
        ).toGiftSendResult(
            purchase = GifticonPurchaseRes(
                gifticonId = 55,
                qrCode = "qr-sensitive-value",
                merchantUid = "merchant-sensitive-value",
            ),
            sentAtMillis = 1_782_012_345_000L,
        )

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("55", gift.giftId)
        assertEquals(1_782_012_345_000L, gift.sentAtMillis)
        assertEquals(false, gift.toString().contains("secret"))
        assertEquals(false, gift.toString().contains("qr-sensitive-value"))
    }

    @Test
    fun missingPurchaseIdMapsToUnknownFailure() {
        val result = GifticonShareRes(
            shareLink = "https://cafeminsu.example/gift/secret",
            deepLink = null,
        ).toGiftSendResult(
            purchase = GifticonPurchaseRes(
                gifticonId = null,
                qrCode = null,
                merchantUid = null,
            ),
            sentAtMillis = 1_782_012_345_000L,
        )

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
