package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.StoreDetailRes
import com.cafeminsu.data.remote.StoreSearchItem
import com.cafeminsu.data.remote.StoreSearchRes
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus

fun StoreSearchRes.toStores(): AppResult<List<Store>> {
    val mapped = stores.orEmpty().map { item ->
        when (val result = item.toStore()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

fun StoreDetailRes.toStore(): AppResult<Store> {
    val storeId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        Store(
            id = storeId.toString(),
            name = name.orEmpty(),
            address = address.orEmpty(),
            phone = phone.orEmpty(),
            distanceMeters = UnknownDistanceMeters,
            latitude = latitude ?: UnknownCoordinate,
            longitude = longitude ?: UnknownCoordinate,
            status = StoreStatus.Open,
            closingTimeLabel = businessHours?.trim()?.takeIf { it.isNotEmpty() },
            amenities = emptyList(),
        ),
    )
}

private fun StoreSearchItem.toStore(): AppResult<Store> {
    val storeId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        Store(
            id = storeId.toString(),
            name = name.orEmpty(),
            address = address.orEmpty(),
            phone = "",
            distanceMeters = UnknownDistanceMeters,
            latitude = UnknownCoordinate,
            longitude = UnknownCoordinate,
            status = StoreStatus.Open,
            closingTimeLabel = null,
            amenities = emptyList(),
        ),
    )
}

private const val UnknownDistanceMeters = 0
private const val UnknownCoordinate = 0.0
