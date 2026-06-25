package com.ssafy.cafeminsu.core.data.repository.menu

import com.ssafy.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.ssafy.cafeminsu.core.common.network.Dispatcher
import com.ssafy.cafeminsu.core.data.model.asEntity
import com.ssafy.cafeminsu.core.data.model.asExternalModel
import com.ssafy.cafeminsu.core.database.dao.MenuDao
import com.ssafy.cafeminsu.core.model.menu.MenuDetail
import com.ssafy.cafeminsu.core.model.menu.MenuSummary
import com.ssafy.cafeminsu.core.network.client.MenuClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OfflineFirstMenuRepository @Inject constructor(
    private val menuClient: MenuClient,
    private val menuDao: MenuDao,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MenuRepository {

    override fun getMenuSummaries(
        storeId: Long,
        category: String,
    ): Flow<List<MenuSummary>> {
        val categoryFilter = category.trim().ifBlank { null }

        return menuDao.getMenuEntities(
            storeId = storeId,
            category = categoryFilter,
        ).map { menuEntities ->
            menuEntities.map { it.asExternalModel() }
        }.flowOn(ioDispatcher)
    }

    override fun getMenu(
        menuId: Long,
    ): Flow<MenuDetail?> {
        return menuDao.getMenuWithOptions(menuId)
            .map { menuWithOptions ->
                menuWithOptions?.asExternalModel()
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun syncMenuSummaries(
        storeId: Long,
    ) {
        withContext(ioDispatcher) {
            val responses = menuClient.getMenus(
                storeId = storeId,
                category = null,
            )

            menuDao.replaceMenus(
                storeId = storeId,
                menuEntities = responses.map { response ->
                    response.asEntity(storeId = storeId)
                },
            )
        }
    }

    override suspend fun syncMenu(
        menuId: Long,
    ) {
        withContext(ioDispatcher) {
            val existingMenu = menuDao.getMenuEntity(menuId) ?: return@withContext
            val response = menuClient.getMenu(menuId)

            menuDao.replaceMenuWithOptions(
                menuEntity = response.asEntity(existingMenu.storeId),
                optionEntities = response.options.map { it.asEntity(response.id) },
            )
        }
    }
}
