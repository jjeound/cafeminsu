package com.cafeminsu.data.local.store

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 매장 목록 오프라인 캐시 행.
 *
 * 캐시 전용이라 도메인 [com.cafeminsu.domain.model.Store] 의 단순 투영만 보관한다.
 * enum(status)·컬렉션(amenities) 은 [StoreCacheMapper] 에서 문자열로 직렬화해 원시 컬럼으로만 저장한다.
 */
@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val distanceMeters: Int,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val closingTimeLabel: String?,
    val amenities: String,
)
