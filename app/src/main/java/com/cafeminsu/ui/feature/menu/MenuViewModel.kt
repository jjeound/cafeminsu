package com.cafeminsu.ui.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MenuUiState> = menuRepository.observeCategories()
        .flatMapLatest { categoryResult ->
            when (categoryResult) {
                is AppResult.Success -> categoryResult.data.toMenuStateFlow()
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

    fun onCategorySelect(id: String) {
        selectedCategoryId.value = id
    }

    fun retry() {
        viewModelScope.launch {
            runCatching {
                menuRepository.refreshMenus()
            }
        }
    }

    private fun List<MenuCategory>.toMenuStateFlow() =
        if (isEmpty()) {
            flowOf(
                MenuUiState.Empty(
                    categories = emptyList(),
                    selectedCategoryId = null,
                    message = "표시할 카테고리가 아직 없어요",
                ),
            )
        } else {
            val categories = sortedBy { it.sortOrder }

            selectedCategoryId
                .map { requestedCategoryId -> categories.selectedCategoryIdFor(requestedCategoryId) }
                .distinctUntilChanged()
                .flatMapLatest { selectedCategoryId ->
                    menuRepository.observeMenus(selectedCategoryId)
                        .map { menuResult ->
                            menuResult.toMenuUiState(
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                            )
                        }
                }
        }

    private fun AppResult<List<MenuItem>>.toMenuUiState(
        categories: List<MenuCategory>,
        selectedCategoryId: String,
    ): MenuUiState =
        when (this) {
            is AppResult.Success -> {
                val categoryTabs = categories.toUiModels()
                val menuItems = data.map { it.toUiModel() }

                if (menuItems.isEmpty()) {
                    MenuUiState.Empty(
                        categories = categoryTabs,
                        selectedCategoryId = selectedCategoryId,
                        message = "선택한 카테고리에 준비된 메뉴가 없어요",
                    )
                } else {
                    MenuUiState.Content(
                        categories = categoryTabs,
                        selectedCategoryId = selectedCategoryId,
                        menus = menuItems,
                    )
                }
            }

            is AppResult.Failure -> error.toMenuError()
        }

    private fun List<MenuCategory>.selectedCategoryIdFor(requestedCategoryId: String?): String =
        firstOrNull { it.id == requestedCategoryId }?.id ?: first().id

    private fun List<MenuCategory>.toUiModels(): List<MenuCategoryUiModel> =
        map { category ->
            MenuCategoryUiModel(
                id = category.id,
                name = category.name,
            )
        }

    private fun MenuItem.toUiModel(): MenuItemUiModel =
        MenuItemUiModel(
            id = id,
            name = name,
            description = description,
            price = basePrice,
            isSoldOut = isSoldOut,
        )

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
    }
}
