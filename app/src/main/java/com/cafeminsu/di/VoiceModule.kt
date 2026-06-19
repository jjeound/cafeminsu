package com.cafeminsu.di

import com.cafeminsu.data.voice.AndroidSpeechRecognizer
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
}
