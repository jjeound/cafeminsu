package com.cafeminsu.di

import com.cafeminsu.data.messaging.FirebaseDeviceMessagingTokenProvider
import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagingModule {
    @Binds
    @Singleton
    abstract fun bindDeviceMessagingTokenProvider(
        provider: FirebaseDeviceMessagingTokenProvider,
    ): DeviceMessagingTokenProvider
}
