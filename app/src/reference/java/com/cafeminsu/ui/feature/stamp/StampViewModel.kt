package com.cafeminsu.ui.feature.stamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.StampCard
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
class StampViewModel @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<StampUiState> = combine(
        sessionRepository.observeAuthState(),
        rewardRepository.observeStampCard(),
    ) { authState, stampResult ->
        mapStampState(
            authState = authState,
            stampResult = stampResult,
        )
    }.catch {
        emit(
            StampUiState.Error(
                message = "스탬프 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = StampUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    private fun mapStampState(
        authState: AuthState,
        stampResult: AppResult<StampCard>,
    ): StampUiState =
        when (authState) {
            AuthState.Unknown -> StampUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> StampUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> when (stampResult) {
                is AppResult.Success -> stampResult.data.toStampUiState()
                is AppResult.Failure -> stampResult.error.toStampError()
            }
        }

    private fun StampCard.toStampUiState(): StampUiState =
        if (history.isEmpty()) {
            StampUiState.Empty(
                currentCount = currentCount,
                goalCount = goalCount,
                message = "아직 적립 내역이 없어요",
            )
        } else {
            StampUiState.Content(
                currentCount = currentCount,
                goalCount = goalCount,
                history = history,
            )
        }

    private fun DomainError.toStampError(): StampUiState.Error =
        StampUiState.Error(
            message = toStampMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toStampMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "스탬프 정보를 찾지 못했어요"
            is DomainError.Payment -> "적립 대상 주문을 확인하지 못했어요"
            is DomainError.Validation -> "스탬프 정보를 확인해 주세요"
            DomainError.Unknown -> "스탬프 정보를 불러오지 못했어요"
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
