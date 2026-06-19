package com.cafeminsu.ui.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onKakaoLoginClick() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)

            when (val result = sessionRepository.login()) {
                is AppResult.Success -> handleLoginSuccess(result.data)
                is AppResult.Failure -> handleLoginFailure(result.error)
            }
        }
    }

    private suspend fun handleLoginSuccess(authState: AuthState) {
        if (authState is AuthState.Authenticated) {
            _uiState.value = LoginUiState(isAuthenticated = true)
            _events.emit(LoginEvent.NavigateHome)
        } else {
            handleLoginFailure(DomainError.Unauthorized)
        }
    }

    private suspend fun handleLoginFailure(error: DomainError) {
        val message = error.toLoginMessage()
        _uiState.value = LoginUiState(errorMessage = message)
        _events.emit(LoginEvent.ShowSnackbar(message))
    }

    private fun DomainError.toLoginMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 취소됐어요"
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            DomainError.Unknown,
            -> "로그인에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
    }
}
