package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonDetailRes
import com.cafeminsu.data.remote.GifticonUseRes
import com.cafeminsu.data.remote.HistoryItem
import com.cafeminsu.data.remote.MyGifticonRes
import com.cafeminsu.data.remote.StampDetailRes
import com.cafeminsu.data.remote.StampSummaryRes
import com.cafeminsu.domain.model.GifticonStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardMapperTest {
    @Test
    fun stampSummaryMapsToDomainCardWithServerIdStrings() {
        val result = StampSummaryRes(
            storeId = 11,
            storeName = "카페민수 강남점",
            count = 7,
        ).toStampCard()

        val stampCard = (result as AppResult.Success).data
        assertEquals("11", stampCard.userId)
        assertEquals(7, stampCard.currentCount)
        assertEquals(10, stampCard.goalCount)
        assertEquals(emptyList<Any>(), stampCard.history)
    }

    @Test
    fun stampDetailMapsHistoriesToDeterministicEvents() {
        val result = StampDetailRes(
            storeId = 11,
            storeName = "카페민수 강남점",
            count = 8,
            histories = listOf(
                HistoryItem(
                    earnedCount = 2,
                    createdAt = "2026-06-20T01:15:30Z",
                ),
            ),
        ).toStampCard()

        val stampCard = (result as AppResult.Success).data
        assertEquals(8, stampCard.currentCount)
        assertEquals("11-1", stampCard.history.single().id)
        assertEquals("server-stamp-11-1", stampCard.history.single().orderId)
        assertEquals(2, stampCard.history.single().count)
        assertEquals(1_781_918_130_000L, stampCard.history.single().createdAtMillis)
    }

    @Test
    fun myGifticonMapsAvailableListItemWithoutSensitiveCodeValues() {
        val result = MyGifticonRes(
            gifticonId = 31,
            balance = 10_000,
            expiresAt = "2026-08-31T15:00:00Z",
        ).toGifticon()

        val gifticon = (result as AppResult.Success).data
        assertEquals("31", gifticon.id)
        assertEquals("₩10,000", gifticon.title)
        assertEquals("", gifticon.barcodeValue)
        assertEquals("", gifticon.qrValue)
        assertEquals(GifticonStatus.Available, gifticon.status)
        assertEquals(1_788_188_400_000L, gifticon.expiresAtMillis)
    }

    @Test
    fun gifticonDetailMapsQrCodeAndStatus() {
        val result = GifticonDetailRes(
            gifticonId = 31,
            amount = 20_000,
            balance = 8_500,
            qrCode = "sensitive-qr-value",
            status = "PARTIAL",
            expiresAt = "2026-08-31T15:00:00Z",
            message = "고마워",
        ).toGifticon()

        val gifticon = (result as AppResult.Success).data
        assertEquals("31", gifticon.id)
        assertEquals("₩8,500", gifticon.title)
        assertEquals("sensitive-qr-value", gifticon.barcodeValue)
        assertEquals("sensitive-qr-value", gifticon.qrValue)
        assertEquals(GifticonStatus.Available, gifticon.status)
    }

    @Test
    fun gifticonUseResponseUpdatesPriorDetailStatus() {
        val detail = GifticonDetailRes(
            gifticonId = 31,
            amount = 10_000,
            balance = 10_000,
            qrCode = "sensitive-qr-value",
            status = "UNUSED",
            expiresAt = "2026-08-31T15:00:00Z",
            message = null,
        )

        val result = GifticonUseRes(
            balanceAfter = 0,
            status = "USED",
        ).toGifticon(previous = detail)

        val gifticon = (result as AppResult.Success).data
        assertEquals("31", gifticon.id)
        assertEquals("₩0", gifticon.title)
        assertEquals(GifticonStatus.Used, gifticon.status)
        assertEquals("sensitive-qr-value", gifticon.qrValue)
    }

    @Test
    fun missingRequiredIdsMapToUnknownError() {
        assertEquals(
            AppResult.Failure(DomainError.Unknown),
            StampSummaryRes(storeId = null, storeName = "강남점", count = 1).toStampCard(),
        )
        assertEquals(
            AppResult.Failure(DomainError.Unknown),
            MyGifticonRes(gifticonId = null, balance = 1_000, expiresAt = null).toGifticon(),
        )
    }
}
