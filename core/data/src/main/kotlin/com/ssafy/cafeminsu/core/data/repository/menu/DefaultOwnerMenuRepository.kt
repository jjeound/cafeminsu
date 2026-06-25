package com.ssafy.cafeminsu.core.data.repository.menu

import com.ssafy.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.ssafy.cafeminsu.core.common.network.Dispatcher
import com.ssafy.cafeminsu.core.data.model.asExternalModel
import com.ssafy.cafeminsu.core.model.menu.MenuDetail
import com.ssafy.cafeminsu.core.model.menu.MenuSummary
import com.ssafy.cafeminsu.core.network.client.MenuClient
import com.ssafy.cafeminsu.core.network.client.OwnerMenuClient
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuAvailabilityRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuCreateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuOptionUpdateRequest
import com.ssafy.cafeminsu.core.network.model.request.menu.MenuUpdateRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultOwnerMenuRepository @Inject constructor(
    private val menuClient: MenuClient,
    private val ownerMenuClient: OwnerMenuClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : OwnerMenuRepository {
    override fun getManagedMenuSummaries(storeId: Long, category: String): Flow<List<MenuSummary>> = flow {
        val filter = category.trim().ifBlank { null }
        emit(menuClient.getMenus(storeId, filter).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getMenu(menuId: Long): Flow<MenuDetail> = flow {
        emit(menuClient.getMenu(menuId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun createMenu(storeId: Long, request: MenuCreateRequest): Flow<Long> = flow {
        emit(ownerMenuClient.createMenu(storeId, request).menuId)
    }.flowOn(ioDispatcher)

    override fun updateMenu(menuId: Long, request: MenuUpdateRequest): Flow<Unit> = flow {
        emit(ownerMenuClient.updateMenu(menuId, request))
    }.flowOn(ioDispatcher)

    override fun updateAvailability(menuId: Long, isAvailable: Boolean): Flow<Unit> = flow {
        emit(ownerMenuClient.updateAvailability(menuId, MenuAvailabilityRequest(isAvailable)))
    }.flowOn(ioDispatcher)

    override fun deleteMenu(menuId: Long): Flow<Unit> = flow {
        emit(ownerMenuClient.deleteMenu(menuId))
    }.flowOn(ioDispatcher)

    override fun addOption(menuId: Long, request: MenuOptionRequest): Flow<Long> = flow {
        emit(ownerMenuClient.addOption(menuId, request).optionId)
    }.flowOn(ioDispatcher)

    override fun updateOption(optionId: Long, request: MenuOptionUpdateRequest): Flow<Unit> = flow {
        emit(ownerMenuClient.updateOption(optionId, request))
    }.flowOn(ioDispatcher)

    override fun deleteOption(optionId: Long): Flow<Unit> = flow {
        emit(ownerMenuClient.deleteOption(optionId))
    }.flowOn(ioDispatcher)
}
