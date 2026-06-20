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
    val options: List<OptionRes>?,
)

@JsonClass(generateAdapter = true)
data class OptionRes(
    val optionId: Long?,
    val optionGroup: String?,
    val optionName: String?,
    val optionPrice: Int?,
)
