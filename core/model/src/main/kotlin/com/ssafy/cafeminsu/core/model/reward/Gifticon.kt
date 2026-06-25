package com.ssafy.cafeminsu.core.model.reward

data class Gifticon(
    val id: String,
    val title: String,
    val barcodeValue: String,
    val qrValue: String,
    val expiresAtMillis: Long,
    val status: GifticonStatus,
)
