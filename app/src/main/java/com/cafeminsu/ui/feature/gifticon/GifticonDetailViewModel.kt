package com.cafeminsu.ui.feature.gifticon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GifticonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rewardRepository: RewardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val gifticonId = savedStateHandle.get<String>(Routes.GIFTICON_ID).orEmpty()
    private val _uiState = MutableStateFlow<GifticonDetailUiState>(GifticonDetailUiState.Loading)
    private var authenticated = false

    val uiState: StateFlow<GifticonDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeAuthState().collect { authState ->
                when (authState) {
                    AuthState.Unknown -> _uiState.value = GifticonDetailUiState.Loading
                    AuthState.Guest,
                    AuthState.Expired,
                    -> {
                        authenticated = false
                        _uiState.value = GifticonDetailUiState.NeedsLogin(
                            message = "로그인이 필요해요",
                            actionLabel = "다시 로그인하기",
                        )
                    }

                    is AuthState.Authenticated -> {
                        authenticated = true
                        if (_uiState.value !is GifticonDetailUiState.Content) {
                            loadGifticon()
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        if (authenticated) {
            loadGifticon()
        } else {
            viewModelScope.launch {
                runCatching {
                    sessionRepository.refreshOnce()
                }
            }
        }
    }

    fun onUse() {
        val content = _uiState.value as? GifticonDetailUiState.Content ?: return
        if (!content.canUse) {
            return
        }

        viewModelScope.launch {
            _uiState.value = when (val result = rewardRepository.markGifticonUsed(content.gifticon.id)) {
                is AppResult.Success -> GifticonDetailUiState.Content(
                    gifticon = result.data,
                    message = "기프티콘을 사용 처리했어요",
                )

                is AppResult.Failure -> result.error.toGifticonDetailError()
            }
        }
    }

    private fun loadGifticon() {
        if (gifticonId.isBlank()) {
            _uiState.value = DomainError.NotFound.toGifticonDetailError()
            return
        }

        _uiState.value = GifticonDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = rewardRepository.getGifticon(gifticonId)) {
                is AppResult.Success -> GifticonDetailUiState.Content(gifticon = result.data)
                is AppResult.Failure -> result.error.toGifticonDetailError()
            }
        }
    }

    private fun DomainError.toGifticonDetailError(): GifticonDetailUiState.Error =
        GifticonDetailUiState.Error(
            message = toGifticonDetailMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toGifticonDetailMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "기프티콘을 찾지 못했어요"
            is DomainError.Payment -> "기프티콘 사용 상태를 확인하지 못했어요"
            is DomainError.Validation -> "기프티콘 정보를 확인해 주세요"
            DomainError.Unknown -> "기프티콘을 불러오지 못했어요"
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
}
