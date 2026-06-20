package com.cafeminsu.ui.feature.gift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.repository.GiftRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GiftViewModel @Inject constructor(
    private val giftRepository: GiftRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val authState = sessionRepository.observeAuthState()
        .catch { emit(AuthState.Expired) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Unknown,
        )
    private val formState = MutableStateFlow(GiftFormState())
    private val _events = MutableSharedFlow<GiftEvent>(extraBufferCapacity = EventBufferCapacity)

    val uiState: StateFlow<GiftUiState> = combine(
        authState,
        formState,
    ) { authState, form ->
        mapGiftState(
            authState = authState,
            form = form,
        )
    }.catch {
        emit(
            GiftUiState.Error(
                message = "선물하기 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = GiftUiState.Loading,
    )
    val events: SharedFlow<GiftEvent> = _events.asSharedFlow()

    fun onAmountSelected(option: GiftAmountOption) {
        formState.update { form ->
            form.copy(selectedAmountOption = option)
        }
    }

    fun onCustomAmountChanged(value: String) {
        formState.update { form ->
            form.copy(customAmountText = value.filter(Char::isDigit))
        }
    }

    fun onChannelSelected(channel: GiftChannel) {
        formState.update { form ->
            form.copy(selectedChannel = channel)
        }
    }

    fun onRecipientChanged(value: String) {
        formState.update { form ->
            form.copy(recipient = value)
        }
    }

    fun onMessageChanged(value: String) {
        formState.update { form ->
            form.copy(message = value.take(MaxMessageLength))
        }
    }

    fun retry() {
        viewModelScope.launch {
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    fun sendGift() {
        val form = formState.value
        val content = form.toContent()

        if (authState.value !is AuthState.Authenticated) {
            _events.tryEmit(GiftEvent.SendFailed("로그인이 필요해요"))
            return
        }

        if (!content.canSend) {
            _events.tryEmit(GiftEvent.SendFailed("받는 사람을 확인해 주세요"))
            return
        }

        formState.update { current -> current.copy(sending = true) }

        viewModelScope.launch {
            val request = GiftSendRequest(
                amount = content.selectedAmount,
                channel = content.selectedChannel,
                recipientRef = content.recipient.trim(),
                message = content.message.ifBlank { null },
            )
            when (val result = sendGiftSafely(request)) {
                is AppResult.Success -> {
                    formState.update { current ->
                        current.copy(
                            sending = false,
                            recipient = "",
                            message = "",
                        )
                    }
                    _events.emit(GiftEvent.SendSucceeded("선물을 보냈어요"))
                }

                is AppResult.Failure -> {
                    formState.update { current -> current.copy(sending = false) }
                    _events.emit(GiftEvent.SendFailed(result.error.toGiftMessage()))
                }
            }
        }
    }

    private suspend fun sendGiftSafely(request: GiftSendRequest) =
        try {
            giftRepository.sendGift(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private fun mapGiftState(
        authState: AuthState,
        form: GiftFormState,
    ): GiftUiState =
        when (authState) {
            AuthState.Unknown -> GiftUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> GiftUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> form.toContent()
        }

    private fun GiftFormState.toContent(): GiftUiState.Content =
        GiftUiState.Content(
            selectedAmountOption = selectedAmountOption,
            selectedChannel = selectedChannel,
            recipient = recipient,
            message = message,
            customAmountText = customAmountText,
            sending = sending,
        )

    private fun DomainError.toGiftMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "선물 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "선물 정보를 확인해 주세요"
            DomainError.Unknown -> "선물 보내기에 실패했어요"
        }

    private data class GiftFormState(
        val selectedAmountOption: GiftAmountOption = GiftAmountOption.TenThousand,
        val selectedChannel: GiftChannel = GiftChannel.KakaoTalk,
        val recipient: String = "",
        val message: String = "오늘 하루 수고 많았어 ☕",
        val customAmountText: String = "",
        val sending: Boolean = false,
    )

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val EventBufferCapacity = 1
        const val MaxMessageLength = 100
    }
}
