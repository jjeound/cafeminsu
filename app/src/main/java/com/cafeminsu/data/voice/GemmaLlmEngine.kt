package com.cafeminsu.data.voice

import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LiteRT-LM(`litertlm-android`)으로 온디바이스 Gemma 4 추론을 수행하는 [VoiceLlmEngine] 구현.
 *
 * 모델은 LiteRT-LM `.litertlm` 번들(오디오 어댑터 포함 멀티모달)이라 MediaPipe LLM Inference로는
 * 못 읽고, 전용 LiteRT-LM 런타임으로 로드한다. 무거운 엔진은 최초 [generate] 호출 시 한 번만
 * lazy 생성(Mutex 가드, `initialize()`는 ~10s)하고, 추론은 IO 디스패처에서 돈다.
 * (백엔드는 안정성을 위해 CPU. GPU 전환 시 매니페스트에 OpenCL 네이티브 라이브러리 선언 필요.)
 */
@Singleton
class GemmaLlmEngine @Inject constructor(
    private val modelProvider: VoiceModelProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VoiceLlmEngine {
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    override suspend fun isReady(): Boolean = modelProvider.isModelAvailable()

    override suspend fun generate(prompt: String): String =
        withContext(ioDispatcher) {
            val conversation = ensureEngine().createConversation()
            try {
                conversation.sendMessage(prompt).contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(separator = "") { it.text }
            } finally {
                conversation.close()
            }
        }

    private suspend fun ensureEngine(): Engine {
        engine?.let { return it }
        return mutex.withLock {
            engine ?: createEngine().also { engine = it }
        }
    }

    private fun createEngine(): Engine {
        val config = EngineConfig(
            modelPath = modelProvider.modelFile().absolutePath,
            backend = Backend.CPU(),
        )
        return Engine(config).apply { initialize() }
    }
}
