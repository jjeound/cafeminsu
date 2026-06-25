package com.ssafy.cafeminsu.core.network.service

import com.ssafy.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuService {
    @GET("api/stores/{storeId}/menus")
    suspend fun getMenus(
        @Path("storeId") storeId: Long,
        @Query("category") category: String? = null,
    ): List<MenuListItemResponse>

    @GET("api/menus/{menuId}")
    suspend fun getMenu(@Path("menuId") menuId: Long): MenuDetailResponse
}
