package com.ssafy.cafeminsu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.cafeminsu.core.data.repository.auth.AuthRepository
import com.ssafy.cafeminsu.core.model.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = mutableUiState.asStateFlow()

    init {
        observeAuthState()
        syncAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                mutableUiState.update { state ->
                    state.copy(authState = authState.toMainActivityAuthState())
                }
            }
        }
    }

    private fun syncAuthState() {
        viewModelScope.launch {
            authRepository.syncAuthState()
                .catch {
                    mutableUiState.update { state ->
                        state.copy(authState = MainActivityAuthState.SignedOut)
                    }
                }
                .collect { authState ->
                    mutableUiState.update { state ->
                        state.copy(authState = authState.toMainActivityAuthState())
                    }
                }
        }
    }
}

data class MainActivityUiState(
    val authState: MainActivityAuthState = MainActivityAuthState.Loading,
)

enum class MainActivityAuthState {
    Loading,
    SignedOut,
    SignedIn,
}

private fun AuthState.toMainActivityAuthState(): MainActivityAuthState =
    when (this) {
        AuthState.Unknown -> MainActivityAuthState.Loading
        AuthState.Guest,
        AuthState.Expired,
        -> MainActivityAuthState.SignedOut
        is AuthState.Authenticated -> MainActivityAuthState.SignedIn
    }
