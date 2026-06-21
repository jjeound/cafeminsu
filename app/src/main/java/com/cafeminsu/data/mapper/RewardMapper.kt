package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonDetailRes
import com.cafeminsu.data.remote.GifticonUseRes
import com.cafeminsu.data.remote.HistoryItem
import com.cafeminsu.data.remote.MyGifticonRes
import com.cafeminsu.data.remote.StampDetailRes
import com.cafeminsu.data.remote.StampSummaryRes
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.StampEvent
import java.text.NumberFormat
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

fun StampSummaryRes.toStampCard(): AppResult<StampCard> {
    val id = storeId ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        StampCard(
            userId = id.toString(),
            currentCount = count ?: DefaultStampCount,
            goalCount = DefaultStampGoal,
            history = emptyList(),
        ),
    )
}

fun StampDetailRes.toStampCard(): AppResult<StampCard> {
    val id = storeId ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        StampCard(
            userId = id.toString(),
            currentCount = count ?: DefaultStampCount,
            goalCount = DefaultStampGoal,
            history = histories.orEmpty().toStampEvents(storeId = id),
        ),
    )
}

fun List<StampSummaryRes>.toRepresentativeStampCard(): AppResult<StampCard> =
    firstOrNull()?.toStampCard()
        ?: AppResult.Success(
            StampCard(
                userId = DefaultStampUserId,
                currentCount = DefaultStampCount,
                goalCount = DefaultStampGoal,
                history = emptyList(),
            ),
        )

fun MyGifticonRes.toGifticon(): AppResult<Gifticon> {
    val id = gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    val balance = balance ?: DefaultGifticonAmount
    return AppResult.Success(
        Gifticon(
            id = id.toString(),
            title = balance.toWonTitle(),
            barcodeValue = "",
            qrValue = "",
            expiresAtMillis = expiresAt.toEpochMillisOrZero(),
            status = GifticonStatus.Available,
        ),
    )
}

fun List<MyGifticonRes>.toGifticons(): AppResult<List<Gifticon>> {
    val mapped = map { item ->
        when (val result = item.toGifticon()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

fun GifticonDetailRes.toGifticon(): AppResult<Gifticon> {
    val id = gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    val status = status.toGifticonStatus()
        ?: return AppResult.Failure(DomainError.Unknown)
    val balance = balance ?: amount ?: DefaultGifticonAmount
    val codeValue = qrCode.orEmpty()

    return AppResult.Success(
        Gifticon(
            id = id.toString(),
            title = balance.toWonTitle(),
            barcodeValue = codeValue,
            qrValue = codeValue,
            expiresAtMillis = expiresAt.toEpochMillisOrZero(),
            status = status,
        ),
    )
}

fun GifticonUseRes.toGifticon(previous: GifticonDetailRes): AppResult<Gifticon> {
    val status = status.toGifticonStatus()
        ?: return AppResult.Failure(DomainError.Unknown)
    val balance = balanceAfter ?: previous.balance ?: previous.amount ?: DefaultGifticonAmount
    return when (val mapped = previous.toGifticon()) {
        is AppResult.Success -> AppResult.Success(
            mapped.data.copy(
                title = balance.toWonTitle(),
                status = status,
            ),
        )

        is AppResult.Failure -> mapped
    }
}

fun GifticonDetailRes.useAmount(): AppResult<Int> {
    val amount = balance ?: amount ?: return AppResult.Failure(DomainError.Unknown)
    return if (amount > 0) {
        AppResult.Success(amount)
    } else {
        AppResult.Failure(DomainError.Validation("usedAmount"))
    }
}

private fun List<HistoryItem>.toStampEvents(storeId: Long): List<StampEvent> =
    mapIndexed { index, history ->
        val sequence = index + 1
        StampEvent(
            id = "$storeId-$sequence",
            orderId = "server-stamp-$storeId-$sequence",
            count = history.earnedCount ?: DefaultHistoryCount,
            createdAtMillis = history.createdAt.toEpochMillisOrZero(),
        )
    }

private fun String?.toGifticonStatus(): GifticonStatus? =
    when (this) {
        "UNUSED",
        "PARTIAL",
        -> GifticonStatus.Available

        "USED" -> GifticonStatus.Used
        "EXPIRED" -> GifticonStatus.Expired
        else -> null
    }

private fun Int.toWonTitle(): String =
    "$WonSymbol${numberFormat.format(this)}"

private fun String?.toEpochMillisOrZero(): Long =
    this?.let { value ->
        try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            DefaultEpochMillis
        }
    } ?: DefaultEpochMillis

private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

private const val DefaultStampGoal = 10
private const val DefaultStampCount = 0
private const val DefaultHistoryCount = 1
private const val DefaultGifticonAmount = 0
private const val DefaultEpochMillis = 0L
private const val DefaultStampUserId = "server-stamp"
private const val WonSymbol = "₩"
