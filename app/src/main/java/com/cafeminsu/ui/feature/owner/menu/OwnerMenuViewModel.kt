package com.cafeminsu.ui.feature.owner.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.OwnerMenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OwnerMenuViewModel @Inject constructor(
    private val ownerMenuRepository: OwnerMenuRepository,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(OwnerMenuFilter.All)
    private val operationError = MutableStateFlow<DomainError?>(null)
    private val processingMenuIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<OwnerMenuUiState> = selectedFilter
        .flatMapLatest { filter ->
            combine(
                ownerMenuRepository.observeManagedMenus(filter.categoryId),
                operationError,
                processingMenuIds,
            ) { menuResult, actionError, processingIds ->
                mapOwnerMenuState(
                    menuResult = menuResult,
                    selectedFilter = filter,
                    operationError = actionError,
                    processingMenuIds = processingIds,
                )
            }
        }
        .catch {
            emit(
                OwnerMenuUiState.Error(
                    message = "메뉴 정보를 불러오지 못했어요",
                    retryable = true,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = OwnerMenuUiState.Loading,
        )

    fun selectFilter(filter: OwnerMenuFilter) {
        selectedFilter.value = filter
        operationError.value = null
    }

    fun retry() {
        operationError.value = null
    }

    fun setSoldOut(menuItemId: String) {
        if (processingMenuIds.value.contains(menuItemId)) return

        val menu = when (val state = uiState.value) {
            is OwnerMenuUiState.Content -> state.menus.firstOrNull { it.id == menuItemId }
            is OwnerMenuUiState.Empty,
            is OwnerMenuUiState.Error,
            OwnerMenuUiState.Loading,
            -> null
        } ?: return
        val nextSoldOut = !menu.isSoldOut

        viewModelScope.launch {
            processingMenuIds.value = processingMenuIds.value + menuItemId
            when (val result = ownerMenuRepository.setSoldOut(menuItemId = menuItemId, soldOut = nextSoldOut)) {
                is AppResult.Success -> {
                    operationError.value = null
                }

                is AppResult.Failure -> {
                    operationError.value = result.error
                }
            }
            processingMenuIds.value = processingMenuIds.value - menuItemId
        }
    }

    private fun mapOwnerMenuState(
        menuResult: AppResult<List<MenuItem>>,
        selectedFilter: OwnerMenuFilter,
        operationError: DomainError?,
        processingMenuIds: Set<String>,
    ): OwnerMenuUiState {
        operationError?.let { return it.toOwnerMenuError() }

        val menus = when (menuResult) {
            is AppResult.Success -> menuResult.data
            is AppResult.Failure -> return menuResult.error.toOwnerMenuError()
        }
        val filters = OwnerMenuFilter.entries.map { filter ->
            OwnerMenuFilterUiModel(
                filter = filter,
                label = filter.label,
                selected = filter == selectedFilter,
            )
        }
        val menuUiModels = menus.map { it.toOwnerMenuUiModel(processingMenuIds) }

        return if (menuUiModels.isEmpty()) {
            OwnerMenuUiState.Empty(
                selectedFilter = selectedFilter,
                filters = filters,
                message = "선택한 카테고리에 메뉴가 없어요",
            )
        } else {
            OwnerMenuUiState.Content(
                selectedFilter = selectedFilter,
                filters = filters,
                menus = menuUiModels,
            )
        }
    }

    private fun MenuItem.toOwnerMenuUiModel(processingMenuIds: Set<String>): OwnerMenuItemUiModel =
        OwnerMenuItemUiModel(
            id = id,
            name = name,
            price = basePrice,
            isSoldOut = isSoldOut,
            statusLabel = if (isSoldOut) "품절" else "판매중",
            isDimmed = isSoldOut,
            isActionInProgress = processingMenuIds.contains(id),
        )

    private fun DomainError.toOwnerMenuError(): OwnerMenuUiState.Error =
        OwnerMenuUiState.Error(
            message = toOwnerMenuMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toOwnerMenuMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "점주 로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "메뉴 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "메뉴 상태를 확인해 주세요"
            DomainError.Unknown -> "메뉴 정보를 불러오지 못했어요"
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
