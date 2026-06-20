package com.cafeminsu.ui.feature.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MyViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val orderRepository: OrderRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val logoutError = MutableStateFlow<DomainError?>(null)
    private val _events = MutableSharedFlow<MyEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<MyEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MyUiState> = combine(
        sessionRepository.observeAuthState(),
        orderRepository.observeOrderHistory(),
        rewardRepository.observeStampCard(),
        rewardRepository.observeGifticons(),
        logoutError,
    ) { authState, orderHistoryResult, stampResult, gifticonResult, currentLogoutError ->
        if (currentLogoutError != null) {
            currentLogoutError.toMyError()
        } else {
            mapMyState(
                authState = authState,
                orderHistoryResult = orderHistoryResult,
                stampResult = stampResult,
                gifticonResult = gifticonResult,
            )
        }
    }.catch {
        emit(
            MyUiState.Error(
                message = "마이페이지를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = MyUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            logoutError.value = null
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            val result = runCatching {
                sessionRepository.clearSession()
            }.getOrElse {
                AppResult.Failure(DomainError.Unknown)
            }

            if (result is AppResult.Failure) {
                logoutError.value = result.error
            } else {
                _events.emit(MyEvent.NavigateLogin)
            }
        }
    }

    private fun mapMyState(
        authState: AuthState,
        orderHistoryResult: AppResult<List<Order>>,
        stampResult: AppResult<StampCard>,
        gifticonResult: AppResult<List<Gifticon>>,
    ): MyUiState {
        return when (authState) {
            AuthState.Unknown -> MyUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> MyUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> when (orderHistoryResult) {
                is AppResult.Success -> {
                    val stampCard = when (stampResult) {
                        is AppResult.Success -> stampResult.data
                        is AppResult.Failure -> return stampResult.error.toMyError()
                    }
                    val gifticons = when (gifticonResult) {
                        is AppResult.Success -> gifticonResult.data
                        is AppResult.Failure -> return gifticonResult.error.toMyError()
                    }
                    authState.user.toMyState(
                        orders = orderHistoryResult.data,
                        stampCard = stampCard,
                        gifticons = gifticons,
                    )
                }

                is AppResult.Failure -> orderHistoryResult.error.toMyError()
            }
        }
    }

    private fun UserProfile.toMyState(
        orders: List<Order>,
        stampCard: StampCard,
        gifticons: List<Gifticon>,
    ): MyUiState =
        MyUiState.Content(
            profile = toMyProfile(),
            stats = MyStatsUiModel(
                orderCount = orders.size,
                stampCount = stampCard.currentCount,
                stampGoalCount = stampCard.goalCount,
                couponCount = gifticons.count { it.status == GifticonStatus.Available },
            ),
            quickMenus = defaultQuickMenus(),
            settings = defaultSettings(),
        )

    private fun UserProfile.toMyProfile(): MyProfileUiModel {
        val safeName = displayName.ifBlank { DefaultDisplayName }
        return MyProfileUiModel(
            displayName = safeName,
            initial = safeName.take(InitialLength),
            tierLabel = DefaultTierLabel,
        )
    }

    private fun defaultQuickMenus(): List<MyQuickMenuUiModel> =
        listOf(
            MyQuickMenuUiModel(id = HistoryQuickMenuId, label = "주문내역"),
            MyQuickMenuUiModel(id = GiftQuickMenuId, label = "선물하기"),
            MyQuickMenuUiModel(id = CouponQuickMenuId, label = "쿠폰"),
            MyQuickMenuUiModel(id = NotificationQuickMenuId, label = "알림설정"),
        )

    private fun defaultSettings(): List<MySettingItemUiModel> =
        listOf(
            MySettingItemUiModel(
                id = TermsSettingId,
                label = "이용 약관",
            ),
            MySettingItemUiModel(
                id = FaqSettingId,
                label = "자주 묻는 질문",
            ),
            MySettingItemUiModel(
                id = SupportSettingId,
                label = "고객센터",
                trailingText = SupportPhone,
            ),
            MySettingItemUiModel(
                id = VersionSettingId,
                label = "버전 정보",
                trailingText = AppVersion,
            ),
            MySettingItemUiModel(
                id = LogoutSettingId,
                label = "로그아웃",
                isDestructive = true,
            ),
        )

    private fun DomainError.toMyError(): MyUiState.Error =
        MyUiState.Error(
            message = toMyMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toMyMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문 내역을 찾지 못했어요"
            is DomainError.Payment -> "주문 내역을 확인하지 못했어요"
            is DomainError.Validation -> "주문 내역을 확인해 주세요"
            DomainError.Unknown -> "마이페이지를 불러오지 못했어요"
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
        const val HistoryQuickMenuId = "history"
        const val GiftQuickMenuId = "gift"
        const val CouponQuickMenuId = "coupon"
        const val NotificationQuickMenuId = "notification_settings"
        const val TermsSettingId = "terms"
        const val FaqSettingId = "faq"
        const val SupportSettingId = "support"
        const val VersionSettingId = "version"
        const val LogoutSettingId = "logout"
        const val SupportPhone = "1588-1234"
        const val AppVersion = "v1.0.0"
        const val DefaultDisplayName = "민수"
        const val DefaultTierLabel = "GOLD"
        const val InitialLength = 1
        const val EventBufferCapacity = 1
        const val StateStopTimeoutMillis = 5_000L
    }
}
