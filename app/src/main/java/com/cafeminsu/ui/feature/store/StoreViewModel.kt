package com.cafeminsu.ui.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus
import com.cafeminsu.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val refreshRequests = MutableStateFlow(0)
    private val selectedStore = MutableStateFlow<StoreDetailUiModel?>(null)
    private val mutableEvents = MutableSharedFlow<StoreEvent>()

    val events: SharedFlow<StoreEvent> = mutableEvents.asSharedFlow()

    // 검색어는 입력 즉시 반영돼야 하므로 매장 목록(비동기 로딩) UiState와 분리해 동기 StateFlow로 노출한다.
    // (TextField 값을 uiState.query에 묶으면 네트워크 응답 전까지 값이 지연돼 입력이 끊긴다.)
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val uiState: StateFlow<StoreUiState> = combine(query, refreshRequests) { currentQuery, _ ->
        currentQuery
    }
        .flatMapLatest { currentQuery ->
            storeRepository.observeNearbyStores(currentQuery)
                .combine(selectedStore) { result, selectedStore ->
                    result.toStoreUiState(
                        query = currentQuery,
                        selectedStore = selectedStore,
                    )
                }
        }
        .catch {
            emit(
                StoreUiState.Error(
                    message = "매장 정보를 불러오지 못했어요",
                    retryable = true,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = StoreUiState.Loading,
        )

    fun onQueryChange(value: String) {
        selectedStore.value = null
        query.value = value
    }

    fun onStoreClick(storeId: String) {
        viewModelScope.launch {
            when (val result = storeRepository.getStore(storeId)) {
                is AppResult.Success -> selectedStore.value = result.data.toDetailUiModel()
                is AppResult.Failure -> Unit
            }
        }
    }

    fun onDismissStoreDetail() {
        selectedStore.value = null
    }

    fun onStartOrder(storeId: String) {
        viewModelScope.launch {
            when (storeRepository.selectStore(storeId)) {
                is AppResult.Success -> mutableEvents.emit(StoreEvent.NavigateToMenu)
                is AppResult.Failure -> Unit
            }
        }
    }

    fun retry() {
        refreshRequests.value += 1
    }

    private fun AppResult<List<Store>>.toStoreUiState(
        query: String,
        selectedStore: StoreDetailUiModel?,
    ): StoreUiState =
        when (this) {
            is AppResult.Success -> {
                val stores = data.map { it.toUiModel() }
                if (stores.isEmpty()) {
                    StoreUiState.Empty(
                        query = query,
                        message = "검색 결과가 없어요",
                    )
                } else {
                    StoreUiState.Content(
                        query = query,
                        stores = stores,
                        selectedStore = selectedStore,
                    )
                }
            }

            is AppResult.Failure -> error.toStoreError()
        }

    private fun Store.toUiModel(): StoreUiModel =
        StoreUiModel(
            id = id,
            name = name,
            address = address,
            distanceLabel = distanceMeters.toDistanceLabel(),
            status = status.toUiModel(),
            statusLabel = status.toListStatusLabel(closingTimeLabel),
            latitude = latitude,
            longitude = longitude,
        )

    private fun Store.toDetailUiModel(): StoreDetailUiModel =
        StoreDetailUiModel(
            id = id,
            name = name,
            statusLabel = status.toDetailStatusLabel(closingTimeLabel),
            address = address,
            phone = phone,
            distanceLabel = "현재 위치에서 ${distanceMeters.toDistanceLabel()}",
            parkingLabel = if (amenities.contains(StoreAmenity.Parking)) {
                "건물 내 30분 무료"
            } else {
                "주차 정보 없음"
            },
            amenities = amenities
                .filterNot { it == StoreAmenity.Parking }
                .map { it.toLabel() },
        )

    private fun Int.toDistanceLabel(): String =
        if (this < MetersInKilometer) {
            "${this}m"
        } else {
            "${this / MetersInKilometer}.${this % MetersInKilometer / DistanceDecimalStep}km"
        }

    private fun StoreStatus.toUiModel(): StoreStatusUiModel =
        when (this) {
            StoreStatus.Open -> StoreStatusUiModel.Open
            StoreStatus.ClosingSoon -> StoreStatusUiModel.ClosingSoon
            StoreStatus.Closed -> StoreStatusUiModel.Closed
        }

    private fun StoreStatus.toListStatusLabel(closingTimeLabel: String?): String =
        when (this) {
            StoreStatus.Open -> "영업중"
            StoreStatus.ClosingSoon -> closingTimeLabel ?: "마감 임박"
            StoreStatus.Closed -> "영업 종료"
        }

    private fun StoreStatus.toDetailStatusLabel(closingTimeLabel: String?): String =
        when (this) {
            StoreStatus.Open -> listOfNotNull("영업중", closingTimeLabel).joinToString(" · ")
            StoreStatus.ClosingSoon -> listOfNotNull("영업중", closingTimeLabel).joinToString(" · ")
            StoreStatus.Closed -> "영업 종료"
        }

    private fun StoreAmenity.toLabel(): String =
        when (this) {
            StoreAmenity.Outlet -> "콘센트"
            StoreAmenity.Wifi -> "Wi-Fi"
            StoreAmenity.DriveThru -> "드라이브스루"
            StoreAmenity.Terrace -> "테라스"
            StoreAmenity.Parking -> "주차"
        }

    private fun DomainError.toStoreError(): StoreUiState.Error =
        StoreUiState.Error(
            message = toStoreMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toStoreMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "매장 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "입력값을 확인해 주세요"
            DomainError.Unknown -> "매장 정보를 불러오지 못했어요"
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
        const val MetersInKilometer = 1_000
        const val DistanceDecimalStep = 100
    }
}
