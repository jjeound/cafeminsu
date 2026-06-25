package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import com.ssafy.cafeminsu.core.network.service.MenuService
import javax.inject.Inject

class MenuClient @Inject constructor(
    private val menuService: MenuService,
) {
    suspend fun getMenus(storeId: Long, category: String? = null): List<MenuListItemResponse> =
        menuService.getMenus(storeId, category)

    suspend fun getMenu(menuId: Long): MenuDetailResponse = menuService.getMenu(menuId)
}
