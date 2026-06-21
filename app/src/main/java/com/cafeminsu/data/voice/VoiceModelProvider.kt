package com.cafeminsu.data.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온디바이스 Gemma 모델 파일의 위치/존재 여부를 해석한다.
 *
 * 모델은 수백 MB↑라 APK에 동봉하지 않는다. 개발 단계에서는 앱 `filesDir/llm/`로 adb-push하고,
 * 실제 배포용 인앱 다운로더는 후속 작업이다. 미존재 시 음성 주문은 `UiState.Error`로 처리된다.
 */
@Singleton
class VoiceModelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun modelFile(): File = File(File(context.filesDir, MODEL_DIR), MODEL_FILE_NAME)

    fun isModelAvailable(): Boolean = modelFile().exists()

    companion object {
        const val MODEL_DIR = "llm"
        const val MODEL_FILE_NAME = "gemma.litertlm"
    }
}
