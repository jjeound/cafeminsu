package com.cafeminsu.di

import com.cafeminsu.data.auth.MockOwnerAuthProvider
import com.cafeminsu.domain.auth.OwnerAuthProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OwnerAuthModule {
    @Binds
    @Singleton
    abstract fun bindOwnerAuthProvider(provider: MockOwnerAuthProvider): OwnerAuthProvider
}
