package com.cafeminsu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.repository.MenuRepository
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
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val rewardRepository: RewardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        menuRepository.observeMenus(),
        rewardRepository.observeStampCard(),
        sessionRepository.observeAuthState(),
    ) { menuResult, stampResult, authState ->
        mapHomeState(
            menuResult = menuResult,
            stampResult = stampResult,
            authState = authState,
        )
    }.catch {
        emit(
            HomeUiState.Error(
                message = "홈 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = HomeUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            runCatching {
                menuRepository.refreshMenus()
                sessionRepository.refreshOnce()
            }
        }
    }

    private fun mapHomeState(
        menuResult: AppResult<List<MenuItem>>,
        stampResult: AppResult<StampCard>,
        authState: AuthState,
    ): HomeUiState {
        val greeting = greetingFor(authState)

        val menus = when (menuResult) {
            is AppResult.Success -> menuResult.data
            is AppResult.Failure -> return menuResult.error.toHomeError()
        }
        val stampCard = when (stampResult) {
            is AppResult.Success -> stampResult.data
            is AppResult.Failure -> return stampResult.error.toHomeError()
        }
        val recommendedMenus = menus
            .asSequence()
            .filterNot { it.isSoldOut }
            .take(RecommendedMenuLimit)
            .map { it.toHomeMenuSummary() }
            .toList()

        return if (recommendedMenus.isEmpty()) {
            HomeUiState.Empty(
                greeting = greeting,
                message = "추천할 메뉴가 아직 없어요",
            )
        } else {
            HomeUiState.Content(
                greeting = greeting,
                recommendedMenus = recommendedMenus,
                stampSummary = stampCard.toHomeStampSummary(),
                ongoingOrder = null,
            )
        }
    }

    private fun greetingFor(authState: AuthState): String =
        when (authState) {
            is AuthState.Authenticated -> "${authState.user.displayName}님, 오늘도 카페민수와 함께해요"
            AuthState.Expired -> "다시 로그인해 주세요"
            AuthState.Guest,
            AuthState.Unknown,
            -> "어서 오세요, 카페민수입니다"
        }

    private fun MenuItem.toHomeMenuSummary(): HomeMenuSummary =
        HomeMenuSummary(
            id = id,
            name = name,
            description = description,
            price = basePrice,
        )

    private fun StampCard.toHomeStampSummary(): HomeStampSummary =
        HomeStampSummary(
            currentCount = currentCount,
            goalCount = goalCount,
        )

    private fun DomainError.toHomeError(): HomeUiState.Error =
        HomeUiState.Error(
            message = toHomeMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toHomeMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "홈 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "입력값을 확인해 주세요"
            DomainError.Unknown -> "홈 정보를 불러오지 못했어요"
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
        const val RecommendedMenuLimit = 3
        const val StateStopTimeoutMillis = 5_000L
    }
}
