package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import com.cafeminsu.core.network.service.MenuService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class MenuClient @Inject constructor(
    private val menuService: MenuService,
) {
    suspend fun getMenus(storeId: Long, category: String? = null): ApiResponse<List<MenuListItemResponse>> =
        menuService.getMenus(storeId, category)

    suspend fun getMenu(menuId: Long): ApiResponse<MenuDetailResponse> = menuService.getMenu(menuId)
}
