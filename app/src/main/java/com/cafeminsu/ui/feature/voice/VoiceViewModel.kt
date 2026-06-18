package com.cafeminsu.ui.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.voice.ParseVoiceOrderUseCase
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.domain.voice.VoiceRecognitionError
import com.cafeminsu.domain.voice.VoiceRecognitionEvent
import com.cafeminsu.domain.voice.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceRecognizer: VoiceRecognizer,
    private val parseVoiceOrderUseCase: ParseVoiceOrderUseCase,
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    private val _events = MutableSharedFlow<VoiceEvent>(extraBufferCapacity = EventBufferCapacity)
    private var recognitionJob: Job? = null
    private var hasAudioPermission = false

    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()
    val events: SharedFlow<VoiceEvent> = _events.asSharedFlow()

    fun onPermissionResult(granted: Boolean) {
        hasAudioPermission = granted
        if (granted) {
            startListening()
        } else {
            recognitionJob?.cancel()
            voiceRecognizer.stop()
            _uiState.value = VoiceUiState.PermissionRequired
        }
    }

    fun onRetry() {
        if (hasAudioPermission) {
            startListening()
        } else {
            _uiState.value = VoiceUiState.PermissionRequired
        }
    }

    fun onConfirm() {
        val parsed = _uiState.value as? VoiceUiState.Parsed ?: return
        if (parsed.items.isEmpty()) {
            _uiState.value = VoiceUiState.Error(
                message = "담을 수 있는 메뉴를 찾지 못했어요",
                transcript = parsed.transcript,
            )
            return
        }

        val soldOutItems = parsed.items.filter { item -> item.isSoldOut }
        if (soldOutItems.isNotEmpty()) {
            _uiState.value = VoiceUiState.Error(
                message = "${soldOutItems.joinToString { item -> item.name }}은 지금 품절이에요",
                transcript = parsed.transcript,
            )
            return
        }

        viewModelScope.launch {
            parsed.items.forEach { item ->
                when (val result = addItemSafely(item)) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _uiState.value = VoiceUiState.Error(
                            message = result.error.toVoiceMessage(),
                            transcript = parsed.transcript,
                        )
                        return@launch
                    }
                }
            }

            _uiState.value = VoiceUiState.AddedToCart(
                transcript = parsed.transcript,
                items = parsed.items,
            )
            _events.emit(VoiceEvent.NavigateToCart)
        }
    }

    private fun startListening() {
        recognitionJob?.cancel()
        _uiState.value = VoiceUiState.Listening(partialText = "")
        recognitionJob = viewModelScope.launch {
            voiceRecognizer.events.collect { event ->
                handleRecognitionEvent(event)
            }
        }
        voiceRecognizer.start()
    }

    private suspend fun handleRecognitionEvent(event: VoiceRecognitionEvent) {
        when (event) {
            is VoiceRecognitionEvent.Partial -> {
                _uiState.value = VoiceUiState.Listening(partialText = event.text)
            }

            is VoiceRecognitionEvent.Final -> parseFinalTranscript(event.text)
            is VoiceRecognitionEvent.Error -> {
                _uiState.value = VoiceUiState.Error(
                    message = event.reason.toVoiceMessage(),
                    transcript = _uiState.value.transcript,
                )
            }

            VoiceRecognitionEvent.EndOfSpeech -> Unit
        }
    }

    private suspend fun parseFinalTranscript(transcript: String) {
        when (val menusResult = loadMenusSafely()) {
            is AppResult.Success -> {
                val parsed = parseVoiceOrderUseCase(
                    transcript = transcript,
                    menu = menusResult.data,
                )
                _uiState.value = VoiceUiState.Parsed(
                    transcript = transcript,
                    items = parsed.items,
                    unmatched = parsed.unmatched,
                )
            }

            is AppResult.Failure -> {
                _uiState.value = VoiceUiState.Error(
                    message = menusResult.error.toVoiceMessage(),
                    transcript = transcript,
                )
            }
        }
    }

    private suspend fun loadMenusSafely() =
        try {
            menuRepository.observeMenus()
                .catch { emit(AppResult.Failure(DomainError.Unknown)) }
                .first()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private suspend fun addItemSafely(item: ParsedOrderItem): AppResult<Cart> =
        try {
            cartRepository.addItem(
                menuItemId = item.menuItemId,
                options = item.selectedOptions,
                quantity = item.quantity,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private fun DomainError.toVoiceMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "메뉴 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "주문 내용을 확인해 주세요"
            DomainError.Unknown -> "음성 주문을 처리하지 못했어요"
        }

    private fun VoiceRecognitionError.toVoiceMessage(): String =
        when (this) {
            VoiceRecognitionError.PermissionDenied -> "마이크 권한이 필요해요"
            VoiceRecognitionError.RecognizerUnavailable -> "이 기기에서 음성 인식을 사용할 수 없어요"
            VoiceRecognitionError.Audio -> "마이크 입력을 확인하지 못했어요"
            VoiceRecognitionError.Network,
            VoiceRecognitionError.NetworkTimeout,
            -> "네트워크 연결을 확인하고 다시 시도해 주세요"

            VoiceRecognitionError.NoMatch,
            VoiceRecognitionError.SpeechTimeout,
            -> "말씀을 알아듣지 못했어요. 다시 시도해 주세요"

            VoiceRecognitionError.Busy -> "음성 인식이 준비 중이에요. 잠시 후 다시 시도해 주세요"
            VoiceRecognitionError.TooManyRequests -> "요청이 많아요. 잠시 후 다시 시도해 주세요"
            VoiceRecognitionError.Client,
            VoiceRecognitionError.Server,
            is VoiceRecognitionError.Unknown,
            -> "음성 인식 중 문제가 생겼어요. 다시 시도해 주세요"
        }

    override fun onCleared() {
        voiceRecognizer.destroy()
        super.onCleared()
    }

    private companion object {
        const val EventBufferCapacity = 1
    }
}
