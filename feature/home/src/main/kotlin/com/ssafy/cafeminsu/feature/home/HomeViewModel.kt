package com.ssafy.cafeminsu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.cafeminsu.core.common.result.Result
import com.ssafy.cafeminsu.core.common.result.asResult
import com.ssafy.cafeminsu.core.data.repository.auth.AuthRepository
import com.ssafy.cafeminsu.core.data.repository.menu.MenuRepository
import com.ssafy.cafeminsu.core.data.repository.order.OrderRepository
import com.ssafy.cafeminsu.core.data.repository.store.StoreRepository
import com.ssafy.cafeminsu.core.model.auth.AuthState
import com.ssafy.cafeminsu.core.model.menu.MenuSummary
import com.ssafy.cafeminsu.core.model.order.OrderStatus
import com.ssafy.cafeminsu.core.model.order.OrderSummary
import com.ssafy.cafeminsu.core.model.store.StoreDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.authState,
        storeRepository.getStore(RECOMMENDED_STORE_ID).asResult(),
        menuRepository.getMenuSummaries(RECOMMENDED_STORE_ID).asResult(),
        orderRepository.getRecentOrders().asResult(),
    ) { authState, storeResult, menusResult, ordersResult ->
        val store = storeResult.dataOrNull()
        val recommendedMenu = menusResult
            .dataOrNull()
            .orEmpty()
            .firstOrNull { it.isAvailable }
            ?.asRecommendedMenu(store)

        val recentOrders = ordersResult
            .dataOrNull()
            .orEmpty()
            .map { it.asHomeRecentOrderUiModel() }

        HomeUiState(
            greeting = authState.asGreeting(),
            selectedStoreName = store?.name,
            recommendedMenu = recommendedMenu,
            recentOrders = recentOrders,
            isLoading = listOf(storeResult, menusResult, ordersResult)
                .any { it is Result.Loading },
            errorMessage = listOf(storeResult, menusResult, ordersResult)
                .firstNotNullOfOrNull { result ->
                    (result as? Result.Error)?.exception?.message
                },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                storeRepository.syncStore(RECOMMENDED_STORE_ID)
                menuRepository.syncMenuSummaries(RECOMMENDED_STORE_ID)
            }
        }

        viewModelScope.launch {
            authRepository.syncAuthState().collect {}
        }
    }

    private fun AuthState.asGreeting(): String =
        when (this) {
            is AuthState.Authenticated -> "${user.displayName}님, 안녕하세요"
            AuthState.Guest -> "안녕하세요"
            AuthState.Unknown -> "카페민수"
            AuthState.Expired -> "다시 로그인이 필요해요"
        }

    private fun MenuSummary.asRecommendedMenu(
        store: StoreDetail?,
    ): HomeRecommendedMenu =
        HomeRecommendedMenu(
            id = id,
            name = name,
            description = category,
            priceLabel = price.toPriceLabel(),
            storeName = store?.name,
        )

    private fun OrderSummary.asHomeRecentOrderUiModel(): HomeRecentOrderUiModel =
        HomeRecentOrderUiModel(
            id = id,
            orderNumber = orderNumber,
            statusLabel = status.asLabel(),
            priceLabel = totalAmount.toPriceLabel(),
            orderedAtLabel = createdAtMillis.toDateTimeLabel(),
        )

    private fun OrderStatus.asLabel(): String =
        when (this) {
            OrderStatus.All -> "전체"
            OrderStatus.PendingPayment -> "결제 대기"
            OrderStatus.Paid -> "결제 완료"
            OrderStatus.Accepted -> "주문 접수"
            OrderStatus.Preparing -> "제조 중"
            OrderStatus.Ready -> "픽업 가능"
            OrderStatus.Completed -> "완료"
            OrderStatus.Cancelled -> "취소"
            OrderStatus.Failed -> "실패"
        }

    private fun Int.toPriceLabel(): String =
        "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

    private fun Long.toDateTimeLabel(): String =
        SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(this))

    private fun <T> Result<T>.dataOrNull(): T? =
        when (this) {
            is Result.Success -> data
            else -> null
        }

    private companion object {
        private const val RECOMMENDED_STORE_ID = 1L
    }
}