package com.cafeminsu.ui.feature.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.NfcCoupon
import com.cafeminsu.domain.nfc.NfcTagCode
import com.cafeminsu.domain.repository.NfcCouponRepository
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
 * 매장 NFC 태그 쿠폰 발급 화면 ViewModel.
 *
 * NFC 하드웨어/Context 비주입 — 상위(step3 리더)가 추출한 raw 페이로드 문자열만 받는다([onTagRead]).
 * 발급은 금전성 액션 — 낙관 금지: 성공 결과/이벤트는 [NfcCouponRepository.claim] 성공 응답 이후에만 방출한다.
 * tagCode/raw 페이로드/발급 결과의 민감 식별자는 로깅하지 않는다(SECURITY §3·§6).
 */
@HiltViewModel
class NfcClaimViewModel @Inject constructor(
    private val nfcCouponRepository: NfcCouponRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NfcClaimUiState())
    val uiState: StateFlow<NfcClaimUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NfcClaimEvent>(extraBufferCapacity = EventBufferCapacity)
    val events: SharedFlow<NfcClaimEvent> = _events.asSharedFlow()

    /**
     * 태그에서 읽은 raw 페이로드로 발급을 시도한다.
     * 진행중(claiming) 재진입(따닥)은 즉시 무시한다. 파싱 불가 페이로드는 호출 없이 인라인 에러만 설정한다.
     */
    fun onTagRead(rawPayload: String) {
        if (_uiState.value.claiming) {
            return
        }
        val code = NfcTagCode.parse(rawPayload)
        if (code == null) {
            _uiState.update { state -> state.copy(errorMessage = InvalidTagMessage) }
            return
        }

        _uiState.update { state -> state.copy(claiming = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = claimSafely(code)) {
                is AppResult.Success -> {
                    val coupon = result.data.toResultUi()
                    _uiState.update { state ->
                        state.copy(claiming = false, claimedCoupon = coupon, errorMessage = null)
                    }
                    _events.emit(NfcClaimEvent.Claimed(coupon))
                }

                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(claiming = false, errorMessage = result.error.toNfcMessage())
                    }
                }
            }
        }
    }

    /** 인라인 에러 안내를 해제한다(스낵바 소비 후 등). */
    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /** 화면 이탈/재태깅 대비 상태 초기화. */
    fun reset() {
        _uiState.value = NfcClaimUiState()
    }

    private suspend fun claimSafely(code: String): AppResult<NfcCoupon> =
        try {
            nfcCouponRepository.claim(code)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private fun DomainError.toNfcMessage(): String =
        when (this) {
            is DomainError.Payment -> when (reason) {
                "nfc-cooldown" -> "오늘은 이미 받았어요. 내일 다시 시도해 주세요"
                "nfc-inactive" -> "사용할 수 없는 태그예요"
                else -> "쿠폰을 발급할 수 없어요"
            }

            DomainError.NotFound -> "유효하지 않은 태그예요"
            DomainError.Unauthorized -> "로그인이 필요해요. 다시 로그인해 주세요"
            is DomainError.Validation -> "태그를 다시 인식해 주세요"
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unknown -> "쿠폰 발급에 실패했어요. 잠시 후 다시 시도해 주세요"
        }

    private companion object {
        const val EventBufferCapacity = 1
        const val InvalidTagMessage = "유효하지 않은 태그예요"
    }
}
