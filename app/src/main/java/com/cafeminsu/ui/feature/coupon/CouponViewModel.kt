package com.cafeminsu.ui.feature.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Coupon
import com.cafeminsu.domain.model.CouponStatus
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.repository.CouponRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CouponViewModel(
    private val rewardRepository: RewardRepository,
    private val couponRepository: CouponRepository,
    private val sessionRepository: SessionRepository,
    private val nowMillis: () -> Long,
) : ViewModel() {
    @Inject
    constructor(
        rewardRepository: RewardRepository,
        couponRepository: CouponRepository,
        sessionRepository: SessionRepository,
    ) : this(
        rewardRepository = rewardRepository,
        couponRepository = couponRepository,
        sessionRepository = sessionRepository,
        nowMillis = { System.currentTimeMillis() },
    )

    val uiState: StateFlow<CouponUiState> = combine(
        sessionRepository.observeAuthState(),
        rewardRepository.observeStampCard(),
        couponRepository.observeCoupons(),
    ) { authState, stampResult, couponResult ->
        mapCouponState(
            authState = authState,
            stampResult = stampResult,
            couponResult = couponResult,
        )
    }.catch {
        emit(
            CouponUiState.Error(
                message = "쿠폰 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = CouponUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    private fun mapCouponState(
        authState: AuthState,
        stampResult: AppResult<StampCard>,
        couponResult: AppResult<List<Coupon>>,
    ): CouponUiState =
        when (authState) {
            AuthState.Unknown -> CouponUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> CouponUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> when {
                stampResult is AppResult.Failure -> stampResult.error.toCouponError()
                couponResult is AppResult.Failure -> couponResult.error.toCouponError()
                stampResult is AppResult.Success && couponResult is AppResult.Success ->
                    CouponUiState.Content(
                        stamp = stampResult.data.toCouponStampUiModel(),
                        coupons = couponResult.data.map { coupon -> coupon.toCouponItemUiModel() },
                    )

                else -> CouponUiState.Error(
                    message = "쿠폰 정보를 불러오지 못했어요",
                    retryable = true,
                )
            }
        }

    private fun StampCard.toCouponStampUiModel(): CouponStampUiModel =
        CouponStampUiModel(
            storeName = DefaultStoreName,
            currentCount = currentCount,
            goalCount = goalCount,
        )

    private fun Coupon.toCouponItemUiModel(): CouponItemUiModel {
        val expiresSoon = status == CouponStatus.Available &&
            expiresAtMillis - nowMillis() in 0..ExpiringSoonWindowMillis
        return CouponItemUiModel(
            id = id,
            title = title,
            expiresLabel = "유효기간 ${formatCouponDate(expiresAtMillis)}",
            available = status == CouponStatus.Available,
            expiringSoon = expiresSoon,
            amount = amount,
            dimmed = status != CouponStatus.Available,
        )
    }

    private fun DomainError.toCouponError(): CouponUiState.Error =
        CouponUiState.Error(
            message = toCouponMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toCouponMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "쿠폰 정보를 찾지 못했어요"
            is DomainError.Payment -> "쿠폰 적용 정보를 확인하지 못했어요"
            is DomainError.Validation -> "쿠폰 정보를 확인해 주세요"
            DomainError.Unknown -> "쿠폰 정보를 불러오지 못했어요"
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

    private fun formatCouponDate(expiresAtMillis: Long): String =
        Instant.ofEpochMilli(expiresAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(couponDateFormatter)

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val DefaultStoreName = "강남점"
        const val ExpiringSoonWindowMillis = 1000L * 60L * 60L * 24L * 7L
    }
}

private val couponDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
