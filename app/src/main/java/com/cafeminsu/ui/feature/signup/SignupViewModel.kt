package com.cafeminsu.ui.feature.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignupViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SignupEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<SignupEvent> = _events.asSharedFlow()

    fun onNicknameChange(value: String) {
        val capped = value.take(SignupUiState.MaxNicknameLength)
        _uiState.update { state ->
            state.copy(
                nickname = capped,
                errorMessage = ruleErrorFor(capped),
            )
        }
    }

    fun onClearClick() {
        _uiState.update { it.copy(nickname = "", errorMessage = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.isNicknameValid || state.isLoading) return
        val nickname = state.nickname

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val check = sessionRepository.checkNickname(nickname)) {
                is AppResult.Success -> {
                    if (!check.data) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = DuplicateMessage) }
                    } else {
                        completeSignup(nickname)
                    }
                }

                is AppResult.Failure -> emitFailure(check.error)
            }
        }
    }

    private suspend fun completeSignup(nickname: String) {
        when (val result = sessionRepository.completeSignup(nickname)) {
            is AppResult.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(SignupEvent.NavigateHome)
            }

            is AppResult.Failure -> emitFailure(result.error)
        }
    }

    private suspend fun emitFailure(error: DomainError) {
        _uiState.update { it.copy(isLoading = false) }
        _events.emit(SignupEvent.ShowSnackbar(error.toSignupMessage()))
    }

    private fun ruleErrorFor(nickname: String): String? =
        when {
            nickname.isEmpty() -> null
            SignupUiState.NicknamePattern.matches(nickname) -> null
            else -> RuleMessage
        }

    private fun DomainError.toSignupMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            DomainError.Unknown,
            -> "닉네임 설정에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
        const val RuleMessage = "한글·영문·숫자 2~10자로 입력해주세요"
        const val DuplicateMessage = "이미 사용 중인 닉네임이에요"
    }
}
