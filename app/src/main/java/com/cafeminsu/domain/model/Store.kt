package com.cafeminsu.domain.model

data class Store(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val distanceMeters: Int,
    val latitude: Double,
    val longitude: Double,
    val status: StoreStatus,
    val closingTimeLabel: String?,
    val amenities: List<StoreAmenity>,
)

enum class StoreStatus {
    Open,
    ClosingSoon,
    Closed,
}

enum class StoreAmenity {
    Outlet,
    Wifi,
    DriveThru,
    Terrace,
    Parking,
}
