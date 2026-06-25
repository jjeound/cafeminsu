package com.cafeminsu.data.local.menu

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 매장별 메뉴 오프라인 캐시 행.
 *
 * 캐시 전용이라 도메인 [com.cafeminsu.domain.model.MenuItem] 의 단순 투영만 보관한다.
 * 중첩 옵션(`options`) 은 [MenuCacheMapper] 에서 Moshi JSON 으로 직렬화해 [optionsJson] 한 컬럼에 담는다.
 * 매장 단위 조회/교체를 위해 [storeId] 에 인덱스를 둔다.
 */
@Entity(
    tableName = "menus",
    indices = [Index("storeId")],
)
data class MenuEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val isVisible: Boolean,
    val optionsJson: String,
)
