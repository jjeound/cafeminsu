package com.cafeminsu.ui.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val storeRepository: StoreRepository,
    cartRepository: CartRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow(RecommendationCategoryId)

    val cartItemCount: StateFlow<Int> = cartRepository.observeCart()
        .map { result ->
            when (result) {
                is AppResult.Success -> result.data.items.sumOf { it.quantity }
                is AppResult.Failure -> 0
            }
        }
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = 0,
        )

    val uiState: StateFlow<MenuUiState> = combine(
        menuRepository.observeCategories(),
        storeRepository.observeSelectedStore(),
        selectedCategoryId,
    ) { categoryResult, selectedStore, requestedCategoryId ->
        MenuSourceState(
            categoryResult = categoryResult,
            storeName = selectedStore.toMenuStoreName(),
            requestedCategoryId = requestedCategoryId,
        )
    }
        .flatMapLatest { source ->
            when (val categoryResult = source.categoryResult) {
                is AppResult.Success -> categoryResult.data.toMenuStateFlow(
                    storeName = source.storeName,
                    requestedCategoryId = source.requestedCategoryId,
                )
                is AppResult.Failure -> flowOf(categoryResult.error.toMenuError())
            }
        }
        .catch {
            emit(
                MenuUiState.Error(
                    message = "메뉴를 불러오지 못했어요",
                    retryable = true,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = MenuUiState.Loading,
        )

    fun onCategorySelect(categoryId: String) {
        selectedCategoryId.value = categoryId
    }

    fun retry() {
        viewModelScope.launch {
            runCatching {
                menuRepository.refreshMenus()
            }
        }
    }

    private fun List<MenuCategory>.toMenuStateFlow(
        storeName: String,
        requestedCategoryId: String,
    ) =
        if (isEmpty()) {
            flowOf(
                MenuUiState.Empty(
                    categories = emptyList(),
                    selectedCategoryId = null,
                    message = "표시할 카테고리가 아직 없어요",
                    storeName = storeName,
                ),
            )
        } else {
            val categories = toMenuCategoryTabs()
            val selectedCategoryId = categories.selectedCategoryIdFor(requestedCategoryId)

            menuRepository.observeMenus(selectedCategoryId.toRepositoryCategoryId())
                .map { menuResult ->
                    menuResult.toMenuUiState(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        storeName = storeName,
                    )
                }
                .distinctUntilChanged()
        }

    private fun AppResult<List<MenuItem>>.toMenuUiState(
        categories: List<MenuCategoryUiModel>,
        selectedCategoryId: String,
        storeName: String,
    ): MenuUiState =
        when (this) {
            is AppResult.Success -> {
                val menuItems = data.map { it.toUiModel() }

                if (menuItems.isEmpty()) {
                    MenuUiState.Empty(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        message = "선택한 카테고리에 준비된 메뉴가 없어요",
                        storeName = storeName,
                    )
                } else {
                    MenuUiState.Content(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        menus = menuItems,
                        storeName = storeName,
                    )
                }
            }

            is AppResult.Failure -> error.toMenuError()
        }

    private fun List<MenuCategoryUiModel>.selectedCategoryIdFor(requestedCategoryId: String): String =
        firstOrNull { it.id == requestedCategoryId }?.id ?: RecommendationCategoryId

    private fun List<MenuCategory>.toMenuCategoryTabs(): List<MenuCategoryUiModel> =
        listOf(MenuCategoryUiModel(id = RecommendationCategoryId, name = "추천")) +
            requiredMenuCategoryTabs() +
            extraMenuCategoryTabs()

    private fun List<MenuCategory>.requiredMenuCategoryTabs(): List<MenuCategoryUiModel> {
        val categoriesById = associateBy { it.id }
        val categoriesByName = associateBy { it.name }
        return RequiredCategorySpecs.map { spec ->
            val category = categoriesById[spec.id] ?: categoriesByName[spec.name]
            MenuCategoryUiModel(
                id = category?.id ?: spec.id,
                name = category?.name ?: spec.name,
            )
        }
    }

    private fun List<MenuCategory>.extraMenuCategoryTabs(): List<MenuCategoryUiModel> {
        val requiredIds = RequiredCategorySpecs.map { it.id }.toSet()
        val requiredNames = RequiredCategorySpecs.map { it.name }.toSet()
        return filterNot { category ->
            category.id in requiredIds || category.name in requiredNames
        }
            .sortedBy { it.sortOrder }
            .map { category ->
                MenuCategoryUiModel(
                    id = category.id,
                    name = category.name,
                )
            }
    }

    private fun String.toRepositoryCategoryId(): String? =
        if (this == RecommendationCategoryId) null else this

    private fun MenuItem.toUiModel(): MenuItemUiModel =
        MenuItemUiModel(
            id = id,
            name = name,
            description = description,
            price = basePrice,
            isSoldOut = isSoldOut,
            imageUrl = imageUrl,
            isEnabled = !isSoldOut,
        )

    private fun Store?.toMenuStoreName(): String =
        this
            ?.name
            ?.removePrefix(CafeMinsuStorePrefix)
            ?.removePrefix(MinsuStorePrefix)
            ?.ifBlank { null }
            ?: DefaultMenuStoreName

    private fun DomainError.toMenuError(): MenuUiState.Error =
        MenuUiState.Error(
            message = toMenuMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toMenuMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "메뉴 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "입력값을 확인해 주세요"
            DomainError.Unknown -> "메뉴를 불러오지 못했어요"
        }

    private fun DomainError.isRetryable(): Boolean =
        when (this) {
            DomainError.Network,
            DomainError.Timeout,
            DomainError.Unknown,
            -> true

            DomainError.Unauthorized,
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            -> false
        }

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val RecommendationCategoryId = "recommendation"
        const val CafeMinsuStorePrefix = "카페민수 "
        const val MinsuStorePrefix = "민수 "
        val RequiredCategorySpecs = listOf(
            MenuCategorySpec(id = "coffee", name = "커피"),
            MenuCategorySpec(id = "noncoffee", name = "논커피"),
            MenuCategorySpec(id = "dessert", name = "디저트"),
            MenuCategorySpec(id = "tea", name = "티"),
        )
    }
}

private data class MenuSourceState(
    val categoryResult: AppResult<List<MenuCategory>>,
    val storeName: String,
    val requestedCategoryId: String,
)

private data class MenuCategorySpec(
    val id: String,
    val name: String,
)
