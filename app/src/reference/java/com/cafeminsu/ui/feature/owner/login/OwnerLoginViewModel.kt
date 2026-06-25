package com.cafeminsu.ui.feature.owner.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.OwnerAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerLoginViewModel(
    private val ownerAuthProvider: OwnerAuthProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OwnerLoginUiState())
    val uiState: StateFlow<OwnerLoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OwnerLoginEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<OwnerLoginEvent> = _events.asSharedFlow()

    fun login(loginId: String, password: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = OwnerLoginUiState(isLoading = true)

            when (val result = ownerAuthProvider.login(loginId = loginId, password = password)) {
                is AppResult.Success -> handleLoginSuccess()
                is AppResult.Failure -> handleLoginFailure(result.error)
            }
        }
    }

    private suspend fun handleLoginSuccess() {
        _uiState.value = OwnerLoginUiState(isAuthenticated = true)
        _events.emit(OwnerLoginEvent.NavigateOwnerHome)
    }

    private suspend fun handleLoginFailure(error: DomainError) {
        val message = error.toOwnerLoginMessage()
        _uiState.value = OwnerLoginUiState(errorMessage = message)
        _events.emit(OwnerLoginEvent.ShowSnackbar(message))
    }

    private fun DomainError.toOwnerLoginMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "아이디 또는 비밀번호를 확인해 주세요"
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            DomainError.Unknown,
            -> "점주 로그인에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
    }
}
