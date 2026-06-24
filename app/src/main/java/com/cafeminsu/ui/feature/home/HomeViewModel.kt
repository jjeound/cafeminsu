package com.cafeminsu.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.RecommendationRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.domain.repository.StoreRepository
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
    private val recommendationRepository: RecommendationRepository,
    private val storeRepository: StoreRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            menuRepository.observeMenus(),
            orderRepository.observeOrderHistory(),
            rewardRepository.observeStampCard(),
            rewardRepository.observeGifticons(),
            sessionRepository.observeAuthState(),
        ) { menuResult, orderResult, stampResult, gifticonResult, authState ->
            HomeInputs(menuResult, orderResult, stampResult, gifticonResult, authState)
        },
        recommendationRepository.observeTodayRecommendation(),
        storeRepository.observeSelectedStore(),
    ) { inputs, recommendationResult, selectedStore ->
        mapHomeState(
            menuResult = inputs.menuResult,
            orderResult = inputs.orderResult,
            stampResult = inputs.stampResult,
            gifticonResult = inputs.gifticonResult,
            authState = inputs.authState,
            recommendationResult = recommendationResult,
            selectedStore = selectedStore,
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
        recommendationResult: AppResult<MenuItem?>,
        selectedStore: Store?,
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
        when (gifticonResult) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return gifticonResult.error.toHomeError()
        }
        // 서버 추천을 우선 사용하되, 실패/빈 결과/매장 미선택 시 기존 메뉴 파생으로 폴백한다.
        val recommendedMenuItem = when (recommendationResult) {
            is AppResult.Success -> recommendationResult.data ?: menus.firstOrNull { !it.isSoldOut }
            is AppResult.Failure -> menus.firstOrNull { !it.isSoldOut }
        }
        val storeName = selectedStore?.name?.takeIf { it.isNotBlank() }
        val recommendedMenu = recommendedMenuItem?.toHomeRecommendedMenu(storeName)

        return if (recommendedMenu == null) {
            HomeUiState.Empty(
                greeting = greeting,
                message = "추천할 메뉴가 아직 없어요",
            )
        } else {
            HomeUiState.Content(
                greeting = greeting,
                recommendedMenu = recommendedMenu,
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

    private fun MenuItem.toHomeRecommendedMenu(storeName: String?): HomeRecommendedMenu =
        HomeRecommendedMenu(
            id = id,
            name = name,
            description = description,
            price = basePrice,
            storeName = storeName,
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

    private data class HomeInputs(
        val menuResult: AppResult<List<MenuItem>>,
        val orderResult: AppResult<List<Order>>,
        val stampResult: AppResult<StampCard>,
        val gifticonResult: AppResult<List<Gifticon>>,
        val authState: AuthState,
    )

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val RecentOrderLimit = 2
        const val DayMillis = 24L * 60L * 60L * 1000L
        const val DefaultUserName = "민수"
    }
}
