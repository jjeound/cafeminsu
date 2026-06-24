package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonPurchaseRes
import com.cafeminsu.data.remote.GifticonShareRes
import com.cafeminsu.domain.model.GiftSendResult

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
