package com.cafeminsu.ui.feature.menu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MenuDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val menuItemId = savedStateHandle.get<String>(Routes.MENU_DETAIL_MENU_ID).orEmpty()
    private val editingCartItemId = savedStateHandle.get<String>(Routes.MENU_DETAIL_CART_ITEM_ID)
        ?.takeIf { it.isNotBlank() }
    private val _uiState = MutableStateFlow<MenuDetailUiState>(MenuDetailUiState.Loading)
    private val _events = MutableSharedFlow<MenuDetailEvent>(extraBufferCapacity = EventBufferCapacity)

    val uiState: StateFlow<MenuDetailUiState> = _uiState.asStateFlow()
    val events: SharedFlow<MenuDetailEvent> = _events.asSharedFlow()

    init {
        loadMenu()
    }

    fun retry() {
        loadMenu()
    }

    fun onOptionToggle(groupId: String, optionId: String) {
        val content = _uiState.value as? MenuDetailUiState.Content ?: return
        val group = content.optionGroups.firstOrNull { it.id == groupId } ?: return
        val option = group.options.firstOrNull { it.id == optionId } ?: return
        if (!option.isAvailable) {
            return
        }

        val currentSelected = content.selectedOptionIdsByGroup[groupId].orEmpty()
        val nextSelected = when (group.selectionMode) {
            MenuDetailSelectionMode.Single -> nextSingleSelection(
                group = group,
                optionId = optionId,
                currentSelected = currentSelected,
            )

            MenuDetailSelectionMode.Multiple -> nextMultipleSelection(
                group = group,
                optionId = optionId,
                currentSelected = currentSelected,
            )
        }

        if (nextSelected == currentSelected) {
            return
        }

        _uiState.value = content.recalculate(
            selectedOptionIdsByGroup = content.selectedOptionIdsByGroup.toMutableMap().apply {
                if (nextSelected.isEmpty()) {
                    remove(groupId)
                } else {
                    put(groupId, nextSelected)
                }
            },
            addStatus = MenuDetailAddStatus.Idle,
        )
    }

    fun onQuantityChange(quantity: Int) {
        val content = _uiState.value as? MenuDetailUiState.Content ?: return
        _uiState.value = content.recalculate(
            quantity = quantity.coerceIn(MinQuantity, MaxQuantity),
            addStatus = MenuDetailAddStatus.Idle,
        )
    }

    fun onAddToCart() {
        val content = _uiState.value as? MenuDetailUiState.Content ?: return
        if (content.isSoldOut) {
            _uiState.value = content.copy(
                addStatus = MenuDetailAddStatus.Error("품절된 메뉴는 담을 수 없어요"),
            )
            return
        }
        if (!content.canAddToCart) {
            _uiState.value = content.copy(
                addStatus = MenuDetailAddStatus.Error("필수 옵션을 선택해 주세요"),
            )
            return
        }

        viewModelScope.launch {
            when (val result = content.commitToCart()) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        (state as? MenuDetailUiState.Content)?.copy(
                            addStatus = MenuDetailAddStatus.Added,
                        ) ?: state
                    }
                    _events.emit(MenuDetailEvent.AddedToCart)
                }

                is AppResult.Failure -> {
                    _uiState.update { state ->
                        (state as? MenuDetailUiState.Content)?.copy(
                            addStatus = MenuDetailAddStatus.Error(result.error.toMenuDetailMessage()),
                        ) ?: state
                    }
                }
            }
        }
    }

    // 편집(장바구니에서 진입)일 때는 새 항목으로 담은 뒤 기존 항목을 제거해 "수정"처럼 동작시킨다.
    private suspend fun MenuDetailUiState.Content.commitToCart(): AppResult<Cart> {
        val result = cartRepository.addItem(
            menuItemId = menuItemId,
            options = selectedOptions(),
            quantity = quantity,
        )
        if (result is AppResult.Success) {
            editingCartItemId?.let { cartRepository.removeItem(it) }
        }
        return result
    }

    private fun loadMenu() {
        if (menuItemId.isBlank()) {
            _uiState.value = DomainError.NotFound.toMenuDetailError()
            return
        }

        _uiState.value = MenuDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = menuRepository.getMenu(menuItemId)) {
                is AppResult.Success -> result.data.toContent(editingCartItemId?.let { findCartItem(it) })
                is AppResult.Failure -> result.error.toMenuDetailError()
            }
        }
    }

    private suspend fun findCartItem(cartItemId: String): CartItem? =
        when (val result = cartRepository.observeCart().first()) {
            is AppResult.Success -> result.data.items.firstOrNull { it.id == cartItemId }
            is AppResult.Failure -> null
        }

    private fun MenuItem.toContent(editingItem: CartItem?): MenuDetailUiState.Content {
        val initialSelections = editingItem
            ?.selectedOptions
            ?.groupBy { option -> option.groupId }
            ?.mapValues { (_, options) -> options.map { it.optionId }.toSet() }
            .orEmpty()
        val initialQuantity = editingItem?.quantity?.coerceIn(MinQuantity, MaxQuantity) ?: MinQuantity

        return MenuDetailUiState.Content(
            menuItemId = id,
            name = name,
            description = description,
            basePrice = basePrice,
            isSoldOut = isSoldOut,
            optionGroups = emptyList(),
            selectedOptionIdsByGroup = emptyMap(),
            quantity = initialQuantity,
            unitPrice = basePrice,
            totalPrice = basePrice,
            canAddToCart = !isSoldOut,
            addStatus = MenuDetailAddStatus.Idle,
            isEditing = editingItem != null,
        ).recalculate(
            selectedOptionIdsByGroup = initialSelections,
            quantity = initialQuantity,
            addStatus = MenuDetailAddStatus.Idle,
            sourceMenu = this,
        )
    }

    private fun MenuDetailUiState.Content.recalculate(
        selectedOptionIdsByGroup: Map<String, Set<String>> = this.selectedOptionIdsByGroup,
        quantity: Int = this.quantity,
        addStatus: MenuDetailAddStatus = this.addStatus,
        sourceMenu: MenuItem? = null,
    ): MenuDetailUiState.Content {
        val sourceGroups = sourceMenu?.options ?: optionGroups.toDomainLikeGroups()
        val groups = sourceGroups.map { group ->
            val selectedOptionIds = selectedOptionIdsByGroup[group.id]
                .orEmpty()
                .filter { selectedId ->
                    group.options.any { option -> option.id == selectedId && option.isAvailable }
                }
                .toSet()
            group.toUiModel(selectedOptionIds)
        }
        val normalizedSelections = groups.associate { group ->
            group.id to group.selectedOptionIds
        }.filterValues { it.isNotEmpty() }
        val selectedExtraPrice = groups.sumOf { group ->
            group.options
                .filter { it.selected }
                .sumOf { it.extraPrice }
        }
        val unitPrice = basePrice + selectedExtraPrice

        return copy(
            optionGroups = groups,
            selectedOptionIdsByGroup = normalizedSelections,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = unitPrice * quantity,
            canAddToCart = !isSoldOut && groups.all { it.isSatisfied },
            addStatus = addStatus,
        )
    }

    private fun MenuOptionGroup.toUiModel(
        selectedOptionIds: Set<String>,
    ): MenuDetailOptionGroupUiModel {
        val minimumSelection = minimumSelection()
        val isSatisfied = selectedOptionIds.size >= minimumSelection &&
            selectedOptionIds.size <= maxSelect

        return MenuDetailOptionGroupUiModel(
            id = id,
            name = name,
            required = required,
            minSelect = minSelect,
            maxSelect = maxSelect,
            selectionMode = if (maxSelect == SingleSelectMax) {
                MenuDetailSelectionMode.Single
            } else {
                MenuDetailSelectionMode.Multiple
            },
            selectedOptionIds = selectedOptionIds,
            options = options.map { option ->
                MenuDetailOptionUiModel(
                    id = option.id,
                    name = option.name,
                    extraPrice = option.extraPrice,
                    isAvailable = option.isAvailable,
                    selected = option.id in selectedOptionIds,
                )
            },
            isSatisfied = isSatisfied,
            helperText = helperText(minimumSelection),
        )
    }

    private fun MenuDetailUiState.Content.selectedOptions(): List<SelectedOption> =
        optionGroups.flatMap { group ->
            group.options
                .filter { option -> option.selected }
                .map { option ->
                    SelectedOption(
                        groupId = group.id,
                        optionId = option.id,
                        name = option.name,
                        extraPrice = option.extraPrice,
                    )
                }
        }

    private fun nextSingleSelection(
        group: MenuDetailOptionGroupUiModel,
        optionId: String,
        currentSelected: Set<String>,
    ): Set<String> =
        if (
            optionId in currentSelected &&
            !group.required &&
            group.minSelect == OptionalMinimumSelection
        ) {
            emptySet()
        } else {
            setOf(optionId)
        }

    private fun nextMultipleSelection(
        group: MenuDetailOptionGroupUiModel,
        optionId: String,
        currentSelected: Set<String>,
    ): Set<String> =
        if (optionId in currentSelected) {
            currentSelected - optionId
        } else if (currentSelected.size < group.maxSelect) {
            currentSelected + optionId
        } else {
            currentSelected
        }

    private fun List<MenuDetailOptionGroupUiModel>.toDomainLikeGroups(): List<MenuOptionGroup> =
        map { group ->
            MenuOptionGroup(
                id = group.id,
                name = group.name,
                required = group.required,
                minSelect = group.minSelect,
                maxSelect = group.maxSelect,
                options = group.options.map { option ->
                    com.cafeminsu.domain.model.MenuOption(
                        id = option.id,
                        name = option.name,
                        extraPrice = option.extraPrice,
                        isAvailable = option.isAvailable,
                    )
                },
            )
        }

    private fun MenuOptionGroup.minimumSelection(): Int =
        if (required) {
            maxOf(RequiredMinimumSelection, minSelect)
        } else {
            minSelect
        }

    private fun MenuOptionGroup.helperText(minimumSelection: Int): String {
        val requirement = if (minimumSelection > OptionalMinimumSelection) {
            "필수"
        } else {
            "선택"
        }
        val countGuide = if (maxSelect == SingleSelectMax) {
            "1개 선택"
        } else {
            "최대 ${maxSelect}개 선택"
        }
        return "$requirement · $countGuide"
    }

    private fun DomainError.toMenuDetailError(): MenuDetailUiState.Error =
        MenuDetailUiState.Error(
            message = toMenuDetailMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toMenuDetailMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "메뉴 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "입력값을 확인해 주세요"
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
        const val EventBufferCapacity = 1
        const val MinQuantity = 1
        const val MaxQuantity = 20
        const val SingleSelectMax = 1
        const val OptionalMinimumSelection = 0
        const val RequiredMinimumSelection = 1
    }
}
