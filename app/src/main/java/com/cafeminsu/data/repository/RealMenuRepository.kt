package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mapper.toMenuCategories
import com.cafeminsu.data.mapper.toMenuItem
import com.cafeminsu.data.mapper.toMenuItems
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.Unauthenticated
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.unwrap
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MenuRepository {
    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        flow {
            emit(
                when (val result = fetchMenuList(categoryId = null)) {
                    is AppResult.Success -> AppResult.Success(result.data.toMenuCategories())
                    is AppResult.Failure -> result
                },
            )
        }.flowOn(ioDispatcher)

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        flow {
            emit(
                when (val result = fetchMenuList(categoryId = categoryId)) {
                    is AppResult.Success -> result.data.toMenuItems()
                    is AppResult.Failure -> result
                },
            )
        }.flowOn(ioDispatcher)

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
        withContext(ioDispatcher) {
            val serverId = menuItemId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (
                val response = runCatchingToAppResult {
                    menuApi.getMenu(serverId)
                }
            ) {
                is AppResult.Success -> response.data.unwrap { it.toMenuItem() }
                is AppResult.Failure -> response
            }
        }

    override suspend fun refreshMenus(): AppResult<Unit> =
        withContext(ioDispatcher) {
            when (val result = fetchMenuList(categoryId = null)) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Failure -> result
            }
        }

    private suspend fun fetchMenuList(categoryId: String?): AppResult<List<MenuListItemRes>> {
        // 서버 메뉴는 매장별(/api/stores/{storeId}/menus)이라 선택 매장이 필요하다.
        // 로그인 직후 등 선택 매장이 없을 때는 에러 대신 빈 목록으로 우아하게 폴백한다
        // (홈이 메뉴 Failure로 에러 화면에 빠지지 않도록).
        val storeId = selectedStoreHolder.current()?.id?.toLongOrNull()
            ?: return AppResult.Success(emptyList())

        return when (
            val response = runCatchingToAppResult {
                menuApi.listByStore(
                    storeId = storeId,
                    category = categoryId.normalizedCategory(),
                )
            }
        ) {
            is AppResult.Success -> response.data.unwrap { AppResult.Success(it) }
            is AppResult.Failure -> response
        }
    }

    private fun String?.normalizedCategory(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
