package com.cafeminsu.domain.voice

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem

/**
 * 음성 인식 transcript를 주문 구조([ParsedOrder])로 해석한다.
 *
 * 규칙기반 파서를 대체하는 온디바이스 LLM(Gemma) 해석의 도메인 계약. 추론은 비동기·실패 가능하므로
 * suspend + [AppResult]로 표현하며, 화면은 실패를 `UiState.Error`로 변환한다.
 */
interface VoiceOrderInterpreter {
    suspend fun interpret(transcript: String, menu: List<MenuItem>): AppResult<ParsedOrder>
}

/**
 * 온디바이스 LLM 추론 엔진 추상화. 네이티브 런타임(LiteRT-LM/MediaPipe)을 인터페이스 뒤로 격리해
 * 프롬프트/응답 매핑 로직을 단위 테스트할 수 있게 한다.
 */
interface VoiceLlmEngine {
    /** 모델이 로드/구동 가능한 상태인지. */
    suspend fun isReady(): Boolean

    /** 프롬프트로 추론을 수행하고 원문 응답을 반환한다. 실패 시 예외를 던진다. */
    suspend fun generate(prompt: String): String
}
