package com.cafeminsu.data.local.menu

import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOptionGroup
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * 도메인 [MenuItem] ↔ 캐시 [MenuEntity] 순수 매핑.
 *
 * 중첩 옵션(`List<MenuOptionGroup>`) 은 Room 컬럼 하나에 담기 위해 Moshi JSON 으로 직렬화한다.
 * 손상된 JSON 은 빈 목록으로 흡수해 캐시 손상이 화면 오류로 번지지 않게 한다.
 */
fun MenuItem.toMenuEntity(storeId: String, moshi: Moshi): MenuEntity =
    MenuEntity(
        id = id,
        storeId = storeId,
        categoryId = categoryId,
        name = name,
        description = description,
        basePrice = basePrice,
        imageUrl = imageUrl,
        isSoldOut = isSoldOut,
        isVisible = isVisible,
        optionsJson = moshi.optionGroupsAdapter().toJson(options),
    )

fun MenuEntity.toMenuItem(moshi: Moshi): MenuItem =
    MenuItem(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        basePrice = basePrice,
        imageUrl = imageUrl,
        isSoldOut = isSoldOut,
        options = moshi.decodeOptionGroups(optionsJson),
        isVisible = isVisible,
    )

private fun Moshi.decodeOptionGroups(json: String): List<MenuOptionGroup> =
    runCatching { optionGroupsAdapter().fromJson(json) }.getOrNull().orEmpty()

private fun Moshi.optionGroupsAdapter(): JsonAdapter<List<MenuOptionGroup>> {
    val type = Types.newParameterizedType(List::class.java, MenuOptionGroup::class.java)
    return adapter(type)
}
