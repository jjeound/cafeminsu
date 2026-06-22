package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuApi {
    @GET("api/stores/{storeId}/menus")
    suspend fun listByStore(
        @Path("storeId") storeId: Long,
        @Query("category") category: String? = null,
    ): BaseResponse<List<MenuListItemRes>>

    @GET("api/menus/{menuId}")
    suspend fun getMenu(
        @Path("menuId") menuId: Long,
    ): BaseResponse<MenuDetailRes>
}

@JsonClass(generateAdapter = true)
data class MenuListItemRes(
    val id: Long?,
    val name: String?,
    val price: Int?,
    val category: String?,
    val imageUrl: String?,
    val isAvailable: Boolean?,
)

@JsonClass(generateAdapter = true)
data class MenuDetailRes(
    val id: Long?,
    val name: String?,
    val description: String?,
    val price: Int?,
    val category: String?,
    val imageUrl: String?,
    val isAvailable: Boolean?,
    val options: List<MenuOptionRes>?,
)

// 라이브 서버 메뉴 상세의 옵션 스키마. 주문 응답(`OptionRes`)과 필드명이 달라 별도 DTO로 둔다:
// 메뉴는 `{id, group, name, additionalPrice, isDefault}`, 주문은 `{optionId, optionGroup, optionName, optionPrice}`.
@JsonClass(generateAdapter = true)
data class MenuOptionRes(
    val id: Long?,
    val group: String?,
    val name: String?,
    val additionalPrice: Int?,
    val isDefault: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class OptionRes(
    val optionId: Long?,
    val optionGroup: String?,
    val optionName: String?,
    val optionPrice: Int?,
)
