package com.ssafy.cafeminsu.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.cafeminsu.core.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = mutableUiState.asStateFlow()

    fun signInWithKakao() {
        viewModelScope.launch {
            authRepository.signInWithKakao()
                .onStart {
                    mutableUiState.update { state ->
                        state.copy(isSigningIn = true, errorMessage = null)
                    }
                }
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isSigningIn = false,
                            errorMessage = "로그인에 실패했어요. 잠시 후 다시 시도해 주세요.",
                        )
                    }
                }
                .onCompletion {
                    mutableUiState.update { state -> state.copy(isSigningIn = false) }
                }
                .collect { }
        }
    }

    fun dismissError() {
        mutableUiState.update { state -> state.copy(errorMessage = null) }
    }
}

data class SignInUiState(
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null,
)
