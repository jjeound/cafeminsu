package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.network.model.request.menu.MenuCreateRequest
import com.cafeminsu.core.network.model.request.menu.MenuOptionRequest
import com.cafeminsu.core.network.model.request.menu.MenuOptionUpdateRequest
import com.cafeminsu.core.network.model.request.menu.MenuUpdateRequest
import kotlinx.coroutines.flow.Flow

interface OwnerMenuRepository {
    fun getManagedMenuSummaries(storeId: Long, category: String = ""): Flow<List<MenuSummary>>

    fun getMenu(menuId: Long): Flow<MenuDetail>

    fun createMenu(storeId: Long, request: MenuCreateRequest): Flow<Long>

    fun updateMenu(menuId: Long, request: MenuUpdateRequest): Flow<Unit>

    fun updateAvailability(menuId: Long, isAvailable: Boolean): Flow<Unit>

    fun deleteMenu(menuId: Long): Flow<Unit>

    fun addOption(menuId: Long, request: MenuOptionRequest): Flow<Long>

    fun updateOption(optionId: Long, request: MenuOptionUpdateRequest): Flow<Unit>

    fun deleteOption(optionId: Long): Flow<Unit>
}
