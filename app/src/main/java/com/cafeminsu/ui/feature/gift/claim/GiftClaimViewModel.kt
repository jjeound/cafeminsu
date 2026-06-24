package com.cafeminsu.ui.feature.gift.claim

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.repository.GiftRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 받는 사람 수동 등록(claim) 화면 ViewModel.
 * 딥링크(`cafeminsu://gift?code=...`)로 받은 코드를 자동 채우고, [GiftRepository.claimGift] 로 등록한다.
 * claimCode 는 로깅하지 않는다(KAKAO_GIFT_BACKEND §1·SECURITY §6).
 */
@HiltViewModel
class GiftClaimViewModel @Inject constructor(
    private val giftRepository: GiftRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GiftClaimUiState(
            code = GiftClaimDeepLink.normalizeCode(savedStateHandle.get<String>(Routes.GIFT_CLAIM_CODE)),
        ),
    )
    val uiState: StateFlow<GiftClaimUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GiftClaimEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<GiftClaimEvent> = _events.asSharedFlow()

    fun onCodeChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                code = GiftClaimDeepLink.sanitizeInput(value),
                errorMessage = null,
            )
        }
    }

    fun claim() {
        val current = _uiState.value
        if (current.submitting) {
            return
        }
        val code = current.code.trim()
        if (!GiftClaimDeepLink.isValidClaimCode(code)) {
            _uiState.update { state -> state.copy(errorMessage = InvalidCodeMessage) }
            return
        }

        _uiState.update { state -> state.copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = claimSafely(code)) {
                is AppResult.Success -> {
                    _uiState.update { state -> state.copy(submitting = false, errorMessage = null) }
                    _events.emit(GiftClaimEvent.Claimed(ClaimedMessage))
                }

                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(submitting = false, errorMessage = result.error.toClaimMessage())
                    }
                }
            }
        }
    }

    private suspend fun claimSafely(code: String): AppResult<*> =
        try {
            giftRepository.claimGift(code)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private fun DomainError.toClaimMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 필요해요. 다시 로그인해 주세요"
            DomainError.NotFound -> "등록할 수 없는 코드예요. 코드를 다시 확인해 주세요"
            is DomainError.Payment -> "이미 등록된 선물이에요"
            is DomainError.Validation -> "등록 코드를 확인해 주세요"
            DomainError.Unknown -> "선물 등록에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
        const val InvalidCodeMessage = "등록 코드를 확인해 주세요"
        const val ClaimedMessage = "선물이 등록됐어요"
    }
}
