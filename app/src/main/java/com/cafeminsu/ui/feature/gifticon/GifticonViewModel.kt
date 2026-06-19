package com.cafeminsu.ui.feature.gifticon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GifticonViewModel @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<GifticonListUiState> = combine(
        sessionRepository.observeAuthState(),
        rewardRepository.observeGifticons(),
    ) { authState, gifticonsResult ->
        mapGifticonListState(
            authState = authState,
            gifticonsResult = gifticonsResult,
        )
    }.catch {
        emit(
            GifticonListUiState.Error(
                message = "기프티콘을 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = GifticonListUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    private fun mapGifticonListState(
        authState: AuthState,
        gifticonsResult: AppResult<List<Gifticon>>,
    ): GifticonListUiState =
        when (authState) {
            AuthState.Unknown -> GifticonListUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> GifticonListUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> when (gifticonsResult) {
                is AppResult.Success -> gifticonsResult.data.toGifticonListState()
                is AppResult.Failure -> gifticonsResult.error.toGifticonListError()
            }
        }

    private fun List<Gifticon>.toGifticonListState(): GifticonListUiState =
        if (isEmpty()) {
            GifticonListUiState.Empty(
                message = "보유한 기프티콘이 없어요",
                actionLabel = "스탬프 보러가기",
            )
        } else {
            GifticonListUiState.Content(gifticons = this)
        }

    private fun DomainError.toGifticonListError(): GifticonListUiState.Error =
        GifticonListUiState.Error(
            message = toGifticonListMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toGifticonListMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "기프티콘을 찾지 못했어요"
            is DomainError.Payment -> "기프티콘 적립 정보를 확인하지 못했어요"
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

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
    }
}
