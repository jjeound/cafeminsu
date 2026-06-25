package com.ssafy.cafeminsu.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.cafeminsu.core.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<SignInUiState>
        field = MutableStateFlow<SignInUiState>(SignInUiState.Idle)

    fun signInWithKakao() {
        if (uiState.value is SignInUiState.SigningIn) return

        viewModelScope.launch {
            authRepository.signInWithKakao()
                .onStart {
                    uiState.value = SignInUiState.SigningIn
                }
                .catch {
                    uiState.value = SignInUiState.Error(
                        message = "로그인에 실패했어요. 잠시 후 다시 시도해 주세요.",
                    )
                }
                .onCompletion {
                    if (uiState.value is SignInUiState.SigningIn) {
                        uiState.value = SignInUiState.Idle
                    }
                }
                .collect {
                    uiState.value = SignInUiState.Idle
                }
        }
    }

    fun dismissError() {
        if (uiState.value is SignInUiState.Error) {
            uiState.value = SignInUiState.Idle
        }
    }
}

sealed interface SignInUiState {
    data object Idle : SignInUiState

    data object SigningIn : SignInUiState

    data class Error(
        val message: String,
    ) : SignInUiState
}