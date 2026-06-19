package com.cafeminsu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.OrderRepository
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
    private val orderRepository: OrderRepository,
    private val rewardRepository: RewardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        menuRepository.observeMenus(),
        orderRepository.observeOrderHistory(),
        rewardRepository.observeStampCard(),
        rewardRepository.observeGifticons(),
        sessionRepository.observeAuthState(),
    ) { menuResult, orderResult, stampResult, gifticonResult, authState ->
        mapHomeState(
            menuResult = menuResult,
            orderResult = orderResult,
            stampResult = stampResult,
            gifticonResult = gifticonResult,
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
        orderResult: AppResult<List<Order>>,
        stampResult: AppResult<StampCard>,
        gifticonResult: AppResult<List<Gifticon>>,
        authState: AuthState,
    ): HomeUiState {
        val greeting = greetingFor(authState)

        val menus = when (menuResult) {
            is AppResult.Success -> menuResult.data
            is AppResult.Failure -> return menuResult.error.toHomeError()
        }
        val orders = when (orderResult) {
            is AppResult.Success -> orderResult.data
            is AppResult.Failure -> return orderResult.error.toHomeError()
        }
        when (stampResult) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return stampResult.error.toHomeError()
        }
        val gifticons = when (gifticonResult) {
            is AppResult.Success -> gifticonResult.data
            is AppResult.Failure -> return gifticonResult.error.toHomeError()
        }
        val recommendedMenu = menus
            .firstOrNull { !it.isSoldOut }
            ?.toHomeRecommendedMenu()

        return if (recommendedMenu == null) {
            HomeUiState.Empty(
                greeting = greeting,
                message = "추천할 메뉴가 아직 없어요",
            )
        } else {
            HomeUiState.Content(
                greeting = greeting,
                recommendedMenu = recommendedMenu,
                availableCouponCount = gifticons.count { it.status == GifticonStatus.Available },
                recentOrders = orders
                    .sortedByDescending { it.createdAtMillis }
                    .mapNotNull { it.toHomeRecentOrderSummary() }
                    .take(RecentOrderLimit),
            )
        }
    }

    private fun greetingFor(authState: AuthState): String =
        "안녕하세요, ${authState.displayNameOrDefault()}님"

    private fun AuthState.displayNameOrDefault(): String =
        when (this) {
            is AuthState.Authenticated -> user.displayName.ifBlank { DefaultUserName }
            AuthState.Expired,
            AuthState.Guest,
            AuthState.Unknown,
            -> DefaultUserName
        }

    private fun MenuItem.toHomeRecommendedMenu(): HomeRecommendedMenu =
        HomeRecommendedMenu(
            id = id,
            name = name,
            description = description,
            price = basePrice,
            originalPrice = basePrice + RecommendedOriginalPriceDelta,
        )

    private fun Order.toHomeRecentOrderSummary(): HomeRecentOrderSummary? {
        val firstItem = items.firstOrNull() ?: return null
        val optionSummary = firstItem.selectedOptions
            .joinToString(separator = " · ") { it.name }
            .ifBlank { "기본 옵션" }

        return HomeRecentOrderSummary(
            orderId = id,
            menuItemId = firstItem.menuItemId,
            menuName = firstItem.name,
            optionSummary = optionSummary,
            orderedAtLabel = toRelativeDayLabel(createdAtMillis),
            totalPrice = totalAmount,
        )
    }

    private fun toRelativeDayLabel(createdAtMillis: Long): String {
        val elapsedDays = (System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L) / DayMillis
        return when (elapsedDays) {
            0L -> "오늘"
            1L -> "어제"
            else -> "${elapsedDays}일 전"
        }
    }

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
        const val StateStopTimeoutMillis = 5_000L
        const val RecommendedOriginalPriceDelta = 500
        const val RecentOrderLimit = 2
        const val DayMillis = 24L * 60L * 60L * 1000L
        const val DefaultUserName = "민수"
    }
}
