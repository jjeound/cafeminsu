package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.GifticonClaimRes
import com.cafeminsu.data.remote.GifticonPurchaseRes
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

// 구매 응답만으로 선물 결과를 구성한다(별도 share API 미사용).
// shareLink(서버 제공 시)·claimCode 만 도메인으로 넘기고, 딥링크/공유 텍스트는 상위 레이어에서 만든다.
fun GifticonPurchaseRes.toGiftSendResult(
    sentAtMillis: Long,
): AppResult<GiftSendResult> {
    val id = gifticonId ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        GiftSendResult(
            giftId = id.toString(),
            sentAtMillis = sentAtMillis,
            shareLink = shareLink,
            claimCode = claimCode,
        ),
    )
}
