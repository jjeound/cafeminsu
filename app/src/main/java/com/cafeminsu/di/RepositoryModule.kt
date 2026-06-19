package com.cafeminsu.di

import com.cafeminsu.data.repository.MockCartRepository
import com.cafeminsu.data.repository.MockMenuRepository
import com.cafeminsu.data.repository.MockNotificationRepository
import com.cafeminsu.data.repository.MockOrderRepository
import com.cafeminsu.data.repository.MockOwnerMenuRepository
import com.cafeminsu.data.repository.MockOwnerOrderRepository
import com.cafeminsu.data.repository.MockPaymentRepository
import com.cafeminsu.data.repository.MockRewardRepository
import com.cafeminsu.data.repository.MockSalesRepository
import com.cafeminsu.data.repository.MockSessionRepository
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.NotificationRepository
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.OwnerMenuRepository
import com.cafeminsu.domain.repository.OwnerOrderRepository
import com.cafeminsu.domain.repository.PaymentRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SalesRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMenuRepository(repository: MockMenuRepository): MenuRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(repository: MockCartRepository): CartRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(repository: MockOrderRepository): OrderRepository

    @Binds
    @Singleton
    abstract fun bindOwnerOrderRepository(repository: MockOwnerOrderRepository): OwnerOrderRepository

    @Binds
    @Singleton
    abstract fun bindOwnerMenuRepository(repository: MockOwnerMenuRepository): OwnerMenuRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(repository: MockSalesRepository): SalesRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(repository: MockPaymentRepository): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindRewardRepository(repository: MockRewardRepository): RewardRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(repository: MockNotificationRepository): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(repository: MockSessionRepository): SessionRepository
}
