package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.core.map
import com.cafeminsu.data.mapper.toMenuCreateReq
import com.cafeminsu.data.mapper.toMenuItem
import com.cafeminsu.data.mapper.toMenuItems
import com.cafeminsu.data.platform.MenuImageData
import com.cafeminsu.data.platform.MenuImageReader
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class RealOwnerMenuRepository @Inject constructor(
    private val ownerMenuApi: OwnerMenuApi,
    // 점주 매장 id 해석은 step 0 의 getMyStores()/MyStoreRes 를 재사용한다(중복 정의 금지).
    private val ownerOrderApi: OwnerOrderApi,
    // 목록 조회(GET stores/{id}/menus)는 public 이라 기존 고객용 MenuApi 를 재사용한다.
    @Unauthenticated private val menuApi: MenuApi,
    // 점주가 고른 로컬 이미지(content://)를 업로드용 바이트로 읽는다.
    private val menuImageReader: MenuImageReader,
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

            // 로컬에서 고른 이미지(content://·file://)는 서버에 업로드해 받은 원격 URL 로 교체한다.
            // 그래야 생성 요청·로컬 스냅샷·메뉴 목록에서 이미지가 실제로 보인다.
            val imageUrl = when (val resolved = resolveImageUrl(draft.imageUrl)) {
                is AppResult.Success -> resolved.data
                is AppResult.Failure -> return@withContext resolved
            }
            val resolvedDraft = draft.copy(imageUrl = imageUrl)

            when (
                val response = runCatchingToAppResult {
                    ownerMenuApi.createMenu(storeId, resolvedDraft.toMenuCreateReq())
                }
            ) {
                is AppResult.Success -> {
                    val menuId = response.data.menuId
                        ?: return@withContext AppResult.Failure(DomainError.Unknown)
                    val created = resolvedDraft.toMenuItem(serverMenuId = menuId)
                    menuSnapshot.value = menuSnapshot.value + created
                    AppResult.Success(created)
                }
                is AppResult.Failure -> response
            }
        }

    // 이미지 URL 을 확정한다: 로컬 URI 면 업로드해 원격 URL 을, 이미 원격(http) URL 이거나 없으면 그대로 반환.
    private suspend fun resolveImageUrl(imageUrl: String?): AppResult<String?> {
        if (imageUrl == null || !imageUrl.isLocalImageUri()) {
            return AppResult.Success(imageUrl)
        }

        // 읽을 수 없으면(권한/포맷 등) 잘못된 content:// 를 서버로 보내지 않고 이미지 없이 진행한다(크래시 금지).
        val image = menuImageReader.read(imageUrl) ?: return AppResult.Success(null)

        return runCatchingToAppResult { ownerMenuApi.uploadMenuImage(image.toFormPart()) }
            .map { it.imageUrl }
    }

    private fun MenuImageData.toFormPart(): MultipartBody.Part {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name = "file", filename = fileName, body = body)
    }

    private fun String.isLocalImageUri(): Boolean =
        startsWith("content://") || startsWith("file://")

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
