package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonPurchaseRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftMapperTest {
    @Test
    fun purchaseResponseMapsIdSentAtShareLinkAndClaimCode() {
        val result = GifticonPurchaseRes(
            gifticonId = 55,
            qrCode = "qr-sensitive-value",
            merchantUid = "merchant-sensitive-value",
            claimCode = "GFT-XXXX-XXXX",
            shareLink = "https://cafeminsu.example/gift?code=GFT-XXXX-XXXX",
        ).toGiftSendResult(sentAtMillis = 1_782_012_345_000L)

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertEquals("55", gift.giftId)
        assertEquals(1_782_012_345_000L, gift.sentAtMillis)
        assertEquals("https://cafeminsu.example/gift?code=GFT-XXXX-XXXX", gift.shareLink)
        assertEquals("GFT-XXXX-XXXX", gift.claimCode)
        // QR 등 민감값은 선물 결과에 실리지 않는다.
        assertEquals(false, gift.toString().contains("qr-sensitive-value"))
    }

    @Test
    fun purchaseResponseWithoutShareLinkKeepsClaimCodeOnly() {
        val result = GifticonPurchaseRes(
            gifticonId = 60,
            qrCode = null,
            merchantUid = null,
            claimCode = "GFT-FROM-PURCHASE",
        ).toGiftSendResult(sentAtMillis = 1_782_012_345_000L)

        assertTrue(result is AppResult.Success)
        val gift = (result as AppResult.Success).data
        assertNull(gift.shareLink)
        assertEquals("GFT-FROM-PURCHASE", gift.claimCode)
    }

    @Test
    fun missingPurchaseIdMapsToUnknownFailure() {
        val result = GifticonPurchaseRes(
            gifticonId = null,
            qrCode = null,
            merchantUid = null,
            claimCode = "GFT-XXXX-XXXX",
        ).toGiftSendResult(sentAtMillis = 1_782_012_345_000L)

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
