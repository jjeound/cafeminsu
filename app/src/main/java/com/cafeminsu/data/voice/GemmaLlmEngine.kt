package com.cafeminsu.data.voice

import android.content.Context
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * MediaPipe LLM Inference(`tasks-genai`)로 온디바이스 Gemma 추론을 수행하는 [VoiceLlmEngine] 구현.
 *
 * 무거운 엔진은 최초 [generate] 호출 시 한 번만 lazy 생성(Mutex 가드)하고, 추론은 IO 디스패처에서 돈다.
 * (Gemma 4 `.litertlm` 사용 시 동일 인터페이스로 LiteRT-LM 런타임으로 교체 가능 — maintenance-only 안내.)
 */
@Singleton
class GemmaLlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelProvider: VoiceModelProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VoiceLlmEngine {
    private val mutex = Mutex()

    @Volatile
    private var inference: LlmInference? = null

    override suspend fun isReady(): Boolean = modelProvider.isModelAvailable()

    override suspend fun generate(prompt: String): String =
        withContext(ioDispatcher) {
            ensureInference().generateResponse(prompt)
        }

    private suspend fun ensureInference(): LlmInference {
        inference?.let { return it }
        return mutex.withLock {
            inference ?: createInference().also { inference = it }
        }
    }

    private fun createInference(): LlmInference {
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelProvider.modelFile().absolutePath)
            .setMaxTokens(MAX_TOKENS)
            .setMaxTopK(MAX_TOP_K)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    private companion object {
        const val MAX_TOKENS = 512
        const val MAX_TOP_K = 40
    }
}
