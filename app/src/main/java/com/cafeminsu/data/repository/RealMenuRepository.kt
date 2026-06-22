package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.menu.MenuLocalDataSource
import com.cafeminsu.data.mapper.toMenuCategories
import com.cafeminsu.data.mapper.toMenuCategoriesFromCache
import com.cafeminsu.data.mapper.toMenuItem
import com.cafeminsu.data.mapper.toMenuItems
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.Unauthenticated
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.MenuRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealMenuRepository @Inject constructor(
    @Unauthenticated
    private val menuApi: MenuApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    private val localDataSource: MenuLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MenuRepository {
    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        flow { emit(loadCategories()) }.flowOn(ioDispatcher)

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        flow { emit(loadMenuItems(categoryId)) }.flowOn(ioDispatcher)

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
        withContext(ioDispatcher) {
            val serverId = menuItemId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (
                val response = runCatchingToAppResult {
                    menuApi.getMenu(serverId)
                }
            ) {
                is AppResult.Success -> response.data.toMenuItem()
                is AppResult.Failure -> response
            }
        }

    override suspend fun refreshMenus(): AppResult<Unit> =
        withContext(ioDispatcher) {
            val storeId = selectedStoreHolder.current()?.id?.toLongOrNull()
                ?: return@withContext AppResult.Success(Unit)
            when (val result = fetchRemoteMenus(storeId, categoryId = null)) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Failure -> result
            }
        }

    private suspend fun loadCategories(): AppResult<List<MenuCategory>> {
        // 선택 매장이 없으면(로그인 직후 등) 캐시를 건드리지 않고 빈 카테고리로 폴백한다.
        val storeId = selectedStoreHolder.current()?.id ?: return AppResult.Success(emptyList())
        val numericStoreId = storeId.toLongOrNull() ?: return AppResult.Success(emptyList())

        return when (val result = fetchRemoteMenus(numericStoreId, categoryId = null)) {
            is AppResult.Success -> AppResult.Success(result.data.toMenuCategories())
            is AppResult.Failure -> {
                // 오프라인 폴백: 캐시가 있으면 캐시 메뉴에서 카테고리를 도출, 없으면 원래 실패 전파.
                val cached = localDataSource.cachedMenus(storeId)
                if (cached.isEmpty()) result else AppResult.Success(cached.toMenuCategoriesFromCache())
            }
        }
    }

    private suspend fun loadMenuItems(categoryId: String?): AppResult<List<MenuItem>> {
        // 서버 메뉴는 매장별(/api/stores/{storeId}/menus)이라 선택 매장이 필요하다.
        // 선택 매장이 없으면 캐시를 읽지도 쓰지도 않고 빈 목록으로 폴백한다(홈이 에러 화면에 빠지지 않도록).
        val storeId = selectedStoreHolder.current()?.id ?: return AppResult.Success(emptyList())
        val numericStoreId = storeId.toLongOrNull() ?: return AppResult.Success(emptyList())

        return when (val result = fetchRemoteMenus(numericStoreId, categoryId)) {
            is AppResult.Success ->
                when (val mapped = result.data.toMenuItems()) {
                    is AppResult.Success -> {
                        // 성공 시 매장 단위로 write-through 후 그대로 방출.
                        localDataSource.replaceMenus(storeId, mapped.data)
                        mapped
                    }
                    is AppResult.Failure -> mapped
                }
            is AppResult.Failure -> {
                // 오프라인 폴백: 캐시가 있으면 카테고리 필터를 적용해 읽기 전용으로 노출, 없으면 실패 전파.
                val cached = localDataSource.cachedMenus(storeId)
                if (cached.isEmpty()) result else AppResult.Success(cached.filterByCategory(categoryId))
            }
        }
    }

    private suspend fun fetchRemoteMenus(
        storeId: Long,
        categoryId: String?,
    ): AppResult<List<MenuListItemRes>> =
        when (
            val response = runCatchingToAppResult {
                menuApi.listByStore(
                    storeId = storeId,
                    category = categoryId.normalizedCategory(),
                )
            }
        ) {
            is AppResult.Success -> AppResult.Success(response.data)
            is AppResult.Failure -> response
        }

    private fun List<MenuItem>.filterByCategory(categoryId: String?): List<MenuItem> {
        val normalized = categoryId.normalizedCategory() ?: return this
        return filter { it.categoryId == normalized }
    }

    private fun String?.normalizedCategory(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
