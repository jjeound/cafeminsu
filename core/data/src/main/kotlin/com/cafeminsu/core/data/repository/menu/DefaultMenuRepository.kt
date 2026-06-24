package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.network.client.MenuClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultMenuRepository @Inject constructor(
    private val client: MenuClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MenuRepository {
    override fun getMenuSummaries(storeId: Long): Flow<List<MenuSummary>> = flow {
        emit(client.getMenus(storeId).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getMenu(id: Long): Flow<MenuDetail> = flow {
        emit(client.getMenu(id).asExternalModel())
    }.flowOn(ioDispatcher)
}