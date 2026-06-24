package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonClaimRes
import com.cafeminsu.data.remote.GifticonPurchaseRes
import com.cafeminsu.data.remote.GifticonShareRes
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.GiftSendResult

fun GifticonClaimRes.toGifticon(): AppResult<Gifticon> {
    val id = gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toClaimGifticonStatus()
        ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        Gifticon(
            id = id.toString(),
            title = title.orEmpty(),
            barcodeValue = barcodeValue.orEmpty(),
            qrValue = qrValue.orEmpty(),
            expiresAtMillis = expiresAtMillis ?: DefaultClaimExpiresAtMillis,
            status = mappedStatus,
        ),
    )
}

private fun String?.toClaimGifticonStatus(): GifticonStatus? =
    when (this) {
        "AVAILABLE",
        "UNUSED",
        "PARTIAL",
        -> GifticonStatus.Available

        "USED" -> GifticonStatus.Used
        "EXPIRED" -> GifticonStatus.Expired
        else -> null
    }

private const val DefaultClaimExpiresAtMillis = 0L

fun GifticonShareRes.toGiftSendResult(
    purchase: GifticonPurchaseRes,
    sentAtMillis: Long,
): AppResult<GiftSendResult> {
    val gifticonId = purchase.gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        GiftSendResult(
            giftId = gifticonId.toString(),
            sentAtMillis = sentAtMillis,
            shareLink = shareLink,
            deepLink = deepLink,
            claimCode = claimCode ?: purchase.claimCode,
        ),
    )
}
