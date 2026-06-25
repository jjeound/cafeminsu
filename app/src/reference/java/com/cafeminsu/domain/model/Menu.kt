package com.cafeminsu.domain.model

import com.squareup.moshi.JsonClass

data class MenuCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val options: List<MenuOptionGroup>,
    val isVisible: Boolean = true,
)

// 메뉴 옵션은 캐시 시 한 컬럼에 Moshi JSON 으로 직렬화하므로 codegen 어댑터를 생성한다(Store 와 동일 패턴).
@JsonClass(generateAdapter = true)
data class MenuOptionGroup(
    val id: String,
    val name: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<MenuOption>,
)

@JsonClass(generateAdapter = true)
data class MenuOption(
    val id: String,
    val name: String,
    val extraPrice: Int,
    val isAvailable: Boolean,
)

data class NewMenuDraft(
    val name: String,
    val categoryId: String,
    val basePrice: Int,
    val description: String,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val options: List<MenuOptionGroup> = emptyList(),
)
