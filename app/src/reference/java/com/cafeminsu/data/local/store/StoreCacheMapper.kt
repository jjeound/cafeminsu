package com.cafeminsu.data.local.store

import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus

/**
 * 도메인 [Store] ↔ 캐시 [StoreEntity] 순수 매핑.
 *
 * enum 은 name 문자열로, amenities 리스트는 구분자로 합친 문자열로 직렬화한다.
 * 미지의 status/amenity 문자열은 기본값으로 흡수해 캐시 손상이 화면 오류로 번지지 않게 한다.
 */
fun Store.toStoreEntity(): StoreEntity =
    StoreEntity(
        id = id,
        name = name,
        address = address,
        phone = phone,
        distanceMeters = distanceMeters,
        latitude = latitude,
        longitude = longitude,
        status = status.name,
        closingTimeLabel = closingTimeLabel,
        amenities = amenities.joinToString(AmenitySeparator) { it.name },
    )

fun StoreEntity.toStore(): Store =
    Store(
        id = id,
        name = name,
        address = address,
        phone = phone,
        distanceMeters = distanceMeters,
        latitude = latitude,
        longitude = longitude,
        status = status.toStoreStatus(),
        closingTimeLabel = closingTimeLabel,
        amenities = amenities.toAmenities(),
    )

fun List<Store>.toStoreEntities(): List<StoreEntity> = map(Store::toStoreEntity)

fun List<StoreEntity>.toStores(): List<Store> = map(StoreEntity::toStore)

private fun String.toStoreStatus(): StoreStatus =
    StoreStatus.entries.firstOrNull { it.name == this } ?: StoreStatus.Open

private fun String.toAmenities(): List<StoreAmenity> =
    if (isEmpty()) {
        emptyList()
    } else {
        split(AmenitySeparator).mapNotNull { name ->
            StoreAmenity.entries.firstOrNull { it.name == name }
        }
    }

private const val AmenitySeparator = ","
