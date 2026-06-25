package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuCreateReq
import com.cafeminsu.data.remote.MenuDetailRes
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.MenuOptionRes
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.NewMenuDraft

fun List<MenuListItemRes>.toMenuCategories(): List<MenuCategory> =
    mapNotNull { it.category.normalizedCategory() }
        .toMenuCategoriesByFirstAppearance()

/**
 * 오프라인 폴백 시 캐시된 도메인 메뉴에서 카테고리를 도출한다.
 * 라이브 목록([toMenuCategories]) 과 동일하게 등장 순서·distinct·빈 카테고리 제외 규칙을 따른다.
 */
fun List<MenuItem>.toMenuCategoriesFromCache(): List<MenuCategory> =
    mapNotNull { it.categoryId.normalizedCategory() }
        .toMenuCategoriesByFirstAppearance()

private fun List<String>.toMenuCategoriesByFirstAppearance(): List<MenuCategory> =
    distinct()
        .mapIndexed { index, category ->
            MenuCategory(
                id = category,
                name = category,
                sortOrder = index + SortOrderOffset,
            )
        }
        .sortedBy { it.sortOrder }

fun List<MenuListItemRes>.toMenuItems(): AppResult<List<MenuItem>> {
    val mapped = map { item ->
        when (val result = item.toMenuItem()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

fun MenuDetailRes.toMenuItem(): AppResult<MenuItem> {
    val menuId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        MenuItem(
            id = menuId.toString(),
            categoryId = category.normalizedCategory().orEmpty(),
            name = name.orEmpty(),
            description = description.orEmpty(),
            basePrice = price ?: DefaultPrice,
            imageUrl = imageUrl,
            isSoldOut = isAvailable != true,
            options = options.orEmpty().toOptionGroups(),
            isVisible = true,
        ),
    )
}

private fun MenuListItemRes.toMenuItem(): AppResult<MenuItem> {
    val menuId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        MenuItem(
            id = menuId.toString(),
            categoryId = category.normalizedCategory().orEmpty(),
            name = name.orEmpty(),
            description = "",
            basePrice = price ?: DefaultPrice,
            imageUrl = imageUrl,
            isSoldOut = isAvailable != true,
            options = emptyList(),
            isVisible = true,
        ),
    )
}

// 점주 메뉴 생성 요청. soldOut 은 서버 isAvailable 의 반전이다(품절=판매불가).
fun NewMenuDraft.toMenuCreateReq(): MenuCreateReq =
    MenuCreateReq(
        name = name,
        description = description,
        price = basePrice,
        category = categoryId,
        imageUrl = imageUrl,
        isAvailable = !isSoldOut,
    )

// 생성 응답의 서버 menuId 를 입혀 도메인 MenuItem 으로 확정한다(나머지 필드는 입력 draft 보존).
fun NewMenuDraft.toMenuItem(serverMenuId: Long): MenuItem =
    MenuItem(
        id = serverMenuId.toString(),
        categoryId = categoryId,
        name = name,
        description = description,
        basePrice = basePrice,
        imageUrl = imageUrl,
        isSoldOut = isSoldOut,
        options = options,
        isVisible = true,
    )

private fun List<MenuOptionRes>.toOptionGroups(): List<MenuOptionGroup> =
    groupBy { it.group.normalizedOptionGroup() }
        .map { (groupName, options) ->
            val mappedOptions = options.mapNotNull { it.toMenuOption() }
            MenuOptionGroup(
                id = groupName,
                name = groupName,
                required = false,
                minSelect = 0,
                // 서버 응답에는 그룹별 선택 제약(maxSelect)이 없다. 옵션 그룹은 단일 선택(1개)으로
                // 강제한다(Mock 데이터와 동일 정책). 다중 선택이 필요하면 서버 계약에 제약 필드가 추가돼야 한다.
                maxSelect = SingleSelectMax,
                options = mappedOptions,
            )
        }

private fun MenuOptionRes.toMenuOption(): MenuOption? {
    val optionId = id ?: return null
    return MenuOption(
        id = optionId.toString(),
        name = name.orEmpty(),
        extraPrice = additionalPrice ?: DefaultPrice,
        isAvailable = true,
    )
}

private fun String?.normalizedCategory(): String? =
    normalize().takeIf { it.isNotEmpty() }

private fun String?.normalizedOptionGroup(): String =
    normalize().ifEmpty { DefaultOptionGroupName }

private fun String?.normalize(): String =
    this?.trim().orEmpty()

private const val SortOrderOffset = 1
private const val DefaultPrice = 0
private const val SingleSelectMax = 1
private const val DefaultOptionGroupName = "옵션"
