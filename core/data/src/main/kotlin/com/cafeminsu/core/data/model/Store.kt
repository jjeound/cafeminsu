package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.cafeminsu.core.database.model.entity.store.StoreEntity
import com.cafeminsu.core.database.model.entity.store.StoreSearchEntity
import com.cafeminsu.core.model.media.ImageSource
import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchItemResponse
import com.cafeminsu.core.model.store.NearbyStoreSummary
import com.cafeminsu.core.model.store.OwnerStoreSummary

fun StoreSearchItemResponse.asEntity(): StoreEntity =
    StoreEntity(id = id, name = name, address = address, imageUrl = imageUrl)

fun StoreSearchItemResponse.asEntity(query: String, page: Int, position: Int): StoreSearchEntity =
    StoreSearchEntity(query = query, page = page, position = position, storeId = id)

fun StoreDetailResponse.asEntity(): StoreDetailEntity =
    StoreDetailEntity(
        storeId = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        businessHours = businessHours,
        imageUrl = imageUrl,
    )

fun StoreEntity.asExternalModel(): StoreSummary =
    StoreSummary(
        id = id,
        name = name,
        address = address,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun StoreDetailEntity.asExternalModel(): StoreDetail =
    StoreDetail(
        id = storeId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        businessHours = businessHours,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun StoreSearchItemResponse.asExternalModel(): StoreSummary =
    StoreSummary(
        id = id,
        name = name,
        address = address,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun StoreDetailResponse.asExternalModel(): StoreDetail =
    StoreDetail(
        id = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        businessHours = businessHours,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun NearbyStoreResponse.asExternalModel(): NearbyStoreSummary =
    NearbyStoreSummary(
        id = id,
        name = name,
        distanceMeters = distance,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun OwnerStoreResponse.asExternalModel(): OwnerStoreSummary =
    OwnerStoreSummary(
        id = id,
        name = name,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )
