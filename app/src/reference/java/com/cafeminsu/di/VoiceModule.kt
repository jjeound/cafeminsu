package com.cafeminsu.di

import com.cafeminsu.data.voice.AndroidSpeechRecognizer
import com.cafeminsu.data.voice.GemmaLlmEngine
import com.cafeminsu.data.voice.GemmaVoiceOrderInterpreter
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.cafeminsu.domain.voice.VoiceOrderInterpreter
import com.cafeminsu.domain.voice.VoiceRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {
    @Binds
    @Singleton
    abstract fun bindVoiceRecognizer(recognizer: AndroidSpeechRecognizer): VoiceRecognizer

    @Binds
    @Singleton
    abstract fun bindVoiceLlmEngine(engine: GemmaLlmEngine): VoiceLlmEngine

    @Binds
    abstract fun bindVoiceOrderInterpreter(interpreter: GemmaVoiceOrderInterpreter): VoiceOrderInterpreter
}
