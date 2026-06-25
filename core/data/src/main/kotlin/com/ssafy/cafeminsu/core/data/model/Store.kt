package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreEntity
import com.ssafy.cafeminsu.core.database.model.entity.store.StoreSearchEntity
import com.ssafy.cafeminsu.core.model.store.NearbyStoreSummary
import com.ssafy.cafeminsu.core.model.store.OwnerStoreSummary
import com.ssafy.cafeminsu.core.model.store.StoreDetail
import com.ssafy.cafeminsu.core.model.store.StoreSummary
import com.ssafy.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.ssafy.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.ssafy.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.store.StoreSearchItemResponse

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
        image = imageUrl.orEmpty(),
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
        image = imageUrl.orEmpty(),
    )

fun StoreSearchItemResponse.asExternalModel(): StoreSummary =
    StoreSummary(
        id = id,
        name = name,
        address = address,
        image = imageUrl.orEmpty(),
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
        image = imageUrl.orEmpty(),
    )

fun NearbyStoreResponse.asExternalModel(): NearbyStoreSummary =
    NearbyStoreSummary(
        id = id,
        name = name,
        distanceMeters = distance,
        image = imageUrl.orEmpty(),
    )

fun OwnerStoreResponse.asExternalModel(): OwnerStoreSummary =
    OwnerStoreSummary(
        id = id,
        name = name,
        image = imageUrl.orEmpty(),
    )
