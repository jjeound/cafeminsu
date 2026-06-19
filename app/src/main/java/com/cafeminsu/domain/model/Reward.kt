package com.cafeminsu.domain.model

data class StampCard(
    val userId: String,
    val currentCount: Int,
    val goalCount: Int,
    val history: List<StampEvent>,
)

data class StampEvent(
    val id: String,
    val orderId: String,
    val count: Int,
    val createdAtMillis: Long,
)

data class Gifticon(
    val id: String,
    val title: String,
    val barcodeValue: String,
    val qrValue: String,
    val expiresAtMillis: Long,
    val status: GifticonStatus,
)

enum class GifticonStatus {
    Available,
    Used,
    Expired,
}
