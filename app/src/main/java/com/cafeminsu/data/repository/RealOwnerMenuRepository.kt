package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.core.map
import com.cafeminsu.data.mapper.toMenuCreateReq
import com.cafeminsu.data.mapper.toMenuItem
import com.cafeminsu.data.mapper.toMenuItems
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.MenuAvailabilityReq
import com.cafeminsu.data.remote.OwnerMenuApi
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.Unauthenticated
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import com.cafeminsu.domain.repository.OwnerMenuRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RealOwnerMenuRepository @Inject constructor(
    private val ownerMenuApi: OwnerMenuApi,
    // 점주 매장 id 해석은 step 0 의 getMyStores()/MyStoreRes 를 재사용한다(중복 정의 금지).
    private val ownerOrderApi: OwnerOrderApi,
    // 목록 조회(GET stores/{id}/menus)는 public 이라 기존 고객용 MenuApi 를 재사용한다.
    @Unauthenticated private val menuApi: MenuApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OwnerMenuRepository {
    // 토글/생성 결과를 마지막 관측 목록에 반영하기 위한 인메모리 스냅샷(서버가 단일 진실, 영속화하지 않음).
    private val menuSnapshot = MutableStateFlow<List<MenuItem>>(emptyList())

    // 최초 1회 API 로딩으로 스냅샷을 채운 뒤, 이후 setSoldOut/setVisible/addMenu 의
    // 스냅샷 변경이 관측자(화면)로 전파되도록 menuSnapshot 을 reactive 하게 흘려보낸다.
    override fun observeManagedMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        flow {
            when (val loaded = loadManagedMenus(categoryId)) {
                is AppResult.Success -> {
                    // 서버 로딩 성공분으로 스냅샷을 시드한 뒤 후속 변경을 계속 방출한다.
                    menuSnapshot.value = loaded.data
                    emitAll(menuSnapshot.map { AppResult.Success(it) })
                }
                // 초기 로딩 실패는 그대로 노출한다(스냅샷은 건드리지 않음).
                is AppResult.Failure -> emit(loaded)
            }
        }.flowOn(ioDispatcher)

    private suspend fun loadManagedMenus(categoryId: String?): AppResult<List<MenuItem>> {
        val storeId = when (val result = resolveStoreId()) {
            // stores/my 가 비어 있으면 메뉴 호출 없이 안전하게 빈 목록을 낸다.
            is AppResult.Success -> result.data ?: return AppResult.Success(emptyList())
            is AppResult.Failure -> return result
        }

        return when (
            val response = runCatchingToAppResult { menuApi.listByStore(storeId, categoryId) }
        ) {
            is AppResult.Success -> response.data.toMenuItems()
            is AppResult.Failure -> response
        }
    }

    override suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem> =
        withContext(ioDispatcher) {
            val menuId = menuItemId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            runCatchingToAppResult {
                ownerMenuApi.setAvailability(menuId, MenuAvailabilityReq(isAvailable = !soldOut))
            }.map {
                // 서버 PATCH 확정 후(낙관적 UI 금지) 마지막 관측분에 품절 상태를 반영해 반환한다.
                applyToSnapshot(menuItemId) { it.copy(isSoldOut = soldOut) }
            }
        }

    // 가시성(isVisible) 토글은 서버 엔드포인트가 없어(SERVER_INTEGRATION '범위 밖') 로컬 스냅샷만 갱신한다.
    override suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem> =
        withContext(ioDispatcher) {
            AppResult.Success(applyToSnapshot(menuItemId) { it.copy(isVisible = visible) })
        }

    override suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem> =
        withContext(ioDispatcher) {
            val storeId = when (val result = resolveStoreId()) {
                is AppResult.Success -> result.data
                    ?: return@withContext AppResult.Failure(DomainError.NotFound)
                is AppResult.Failure -> return@withContext result
            }

            when (
                val response = runCatchingToAppResult {
                    ownerMenuApi.createMenu(storeId, draft.toMenuCreateReq())
                }
            ) {
                is AppResult.Success -> {
                    val menuId = response.data.menuId
                        ?: return@withContext AppResult.Failure(DomainError.Unknown)
                    val created = draft.toMenuItem(serverMenuId = menuId)
                    menuSnapshot.value = menuSnapshot.value + created
                    AppResult.Success(created)
                }
                is AppResult.Failure -> response
            }
        }

    // 점주 매장은 stores/my 첫 매장으로 해석한다(step 0 RealOwnerOrderRepository 와 동일).
    private suspend fun resolveStoreId(): AppResult<Long?> =
        when (val response = runCatchingToAppResult { ownerOrderApi.getMyStores() }) {
            is AppResult.Success -> AppResult.Success(response.data.firstOrNull()?.id)
            is AppResult.Failure -> response
        }

    // 마지막 관측 스냅샷에서 항목을 찾아 변형·갱신해 반환한다. 없으면 id 만 가진 최소 항목으로 폴백.
    private fun applyToSnapshot(menuItemId: String, transform: (MenuItem) -> MenuItem): MenuItem {
        val current = menuSnapshot.value
        val existing = current.firstOrNull { it.id == menuItemId }
        val updated = transform(existing ?: minimalMenu(menuItemId))
        if (existing != null) {
            menuSnapshot.value = current.map { if (it.id == menuItemId) updated else it }
        }
        return updated
    }

    private fun minimalMenu(menuItemId: String): MenuItem =
        MenuItem(
            id = menuItemId,
            categoryId = EmptyText,
            name = EmptyText,
            description = EmptyText,
            basePrice = NoPrice,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        )

    private companion object {
        const val EmptyText = ""
        const val NoPrice = 0
    }
}
