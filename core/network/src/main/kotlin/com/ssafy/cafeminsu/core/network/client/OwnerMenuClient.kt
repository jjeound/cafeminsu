package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.request.menu.MenuAvailabilityRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuCreateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionUpdateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuUpdateRequest
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuCreateResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuOptionCreateResponse
import com.ssafy.cafeminsu.core.network.service.OwnerMenuService
import javax.inject.Inject

class OwnerMenuClient @Inject constructor(
    private val ownerMenuService: OwnerMenuService,
) {
    suspend fun createMenu(storeId: Long, request: MenuCreateRequest): MenuCreateResponse =
        ownerMenuService.createMenu(storeId, request)

    suspend fun updateMenu(menuId: Long, request: MenuUpdateRequest) {
        ownerMenuService.updateMenu(menuId, request)
    }

    suspend fun updateAvailability(menuId: Long, request: MenuAvailabilityRequest) {
        ownerMenuService.updateAvailability(menuId, request)
    }

    suspend fun deleteMenu(menuId: Long) {
        ownerMenuService.deleteMenu(menuId)
    }

    suspend fun addOption(menuId: Long, request: MenuOptionRequest): MenuOptionCreateResponse =
        ownerMenuService.addOption(menuId, request)

    suspend fun updateOption(optionId: Long, request: MenuOptionUpdateRequest) {
        ownerMenuService.updateOption(optionId, request)
    }

    suspend fun deleteOption(optionId: Long) {
        ownerMenuService.deleteOption(optionId)
    }
}
