package com.cafeminsu.ui.feature.owner.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.NewMenuDraft
import com.cafeminsu.domain.repository.OwnerMenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerMenuAddViewModel @Inject constructor(
    private val ownerMenuRepository: OwnerMenuRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OwnerMenuAddUiState())
    val uiState: StateFlow<OwnerMenuAddUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OwnerMenuAddEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<OwnerMenuAddEvent> = _events.asSharedFlow()

    fun onImagePicked(uri: String?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun onCategorySelected(category: OwnerMenuAddCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value.take(OwnerMenuAddUiState.MaxNameLength), errorMessage = null) }
    }

    fun onPriceChange(value: String) {
        val digits = value.filter(Char::isDigit).take(OwnerMenuAddUiState.MaxPriceLength)
        _uiState.update { it.copy(priceInput = digits, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onSaleToggle(onSale: Boolean) {
        _uiState.update { it.copy(onSale = onSale) }
    }

    fun onAddOptionGroup() {
        val newGroup = OwnerMenuOptionGroupInput(
            id = newId(),
            options = listOf(OwnerMenuOptionInput(id = newId())),
        )
        _uiState.update { it.copy(optionGroups = it.optionGroups + newGroup) }
    }

    fun onRemoveOptionGroup(groupId: String) {
        _uiState.update { it.copy(optionGroups = it.optionGroups.filterNot { group -> group.id == groupId }) }
    }

    fun onOptionGroupNameChange(groupId: String, value: String) {
        updateGroup(groupId) { it.copy(name = value.take(OwnerMenuAddUiState.MaxNameLength)) }
    }

    fun onAddOption(groupId: String) {
        updateGroup(groupId) { it.copy(options = it.options + OwnerMenuOptionInput(id = newId())) }
    }

    fun onRemoveOption(groupId: String, optionId: String) {
        updateGroup(groupId) { group ->
            group.copy(options = group.options.filterNot { it.id == optionId })
        }
    }

    fun onOptionNameChange(groupId: String, optionId: String, value: String) {
        updateOption(groupId, optionId) { it.copy(name = value.take(OwnerMenuAddUiState.MaxNameLength)) }
    }

    fun onOptionPriceChange(groupId: String, optionId: String, value: String) {
        val digits = value.filter(Char::isDigit).take(OwnerMenuAddUiState.MaxPriceLength)
        updateOption(groupId, optionId) { it.copy(priceInput = digits) }
    }

    private fun updateGroup(
        groupId: String,
        transform: (OwnerMenuOptionGroupInput) -> OwnerMenuOptionGroupInput,
    ) {
        _uiState.update { state ->
            state.copy(
                optionGroups = state.optionGroups.map { group ->
                    if (group.id == groupId) transform(group) else group
                },
                errorMessage = null,
            )
        }
    }

    private fun updateOption(
        groupId: String,
        optionId: String,
        transform: (OwnerMenuOptionInput) -> OwnerMenuOptionInput,
    ) {
        updateGroup(groupId) { group ->
            group.copy(
                options = group.options.map { option ->
                    if (option.id == optionId) transform(option) else option
                },
            )
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val draft = NewMenuDraft(
                name = state.trimmedName,
                categoryId = state.category.categoryId,
                basePrice = state.price ?: 0,
                description = state.description.trim(),
                imageUrl = state.imageUri,
                isSoldOut = !state.onSale,
                options = state.optionGroups.toOptionGroups(),
            )

            when (val result = ownerMenuRepository.addMenu(draft)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(OwnerMenuAddEvent.Saved)
                }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(OwnerMenuAddEvent.ShowSnackbar(result.error.toAddMenuMessage()))
                }
            }
        }
    }

    private fun List<OwnerMenuOptionGroupInput>.toOptionGroups(): List<MenuOptionGroup> =
        map { group ->
            MenuOptionGroup(
                id = group.id,
                name = group.trimmedName,
                required = false,
                minSelect = 0,
                maxSelect = group.options.size,
                options = group.options.map { option ->
                    MenuOption(
                        id = option.id,
                        name = option.trimmedName,
                        extraPrice = option.extraPrice,
                        isAvailable = true,
                    )
                },
            )
        }

    private fun newId(): String = UUID.randomUUID().toString()

    private fun DomainError.toAddMenuMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "점주 로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            DomainError.Unknown,
            -> "메뉴 추가에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
    }
}
