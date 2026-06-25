package com.ssafy.cafeminsu.core.data.di

import com.ssafy.cafeminsu.core.data.repository.auth.AuthRepository
import com.ssafy.cafeminsu.core.data.repository.auth.DefaultAuthRepository
import com.ssafy.cafeminsu.core.data.repository.cart.CartRepository
import com.ssafy.cafeminsu.core.data.repository.cart.DefaultCartRepository
import com.ssafy.cafeminsu.core.data.repository.menu.OfflineFirstMenuRepository
import com.ssafy.cafeminsu.core.data.repository.menu.DefaultOwnerMenuRepository
import com.ssafy.cafeminsu.core.data.repository.menu.MenuRepository
import com.ssafy.cafeminsu.core.data.repository.menu.OwnerMenuRepository
import com.ssafy.cafeminsu.core.data.repository.notification.DefaultNotificationRepository
import com.ssafy.cafeminsu.core.data.repository.notification.NotificationRepository
import com.ssafy.cafeminsu.core.data.repository.order.DefaultOrderRepository
import com.ssafy.cafeminsu.core.data.repository.order.DefaultOwnerOrderRepository
import com.ssafy.cafeminsu.core.data.repository.order.OrderRepository
import com.ssafy.cafeminsu.core.data.repository.order.OwnerOrderRepository
import com.ssafy.cafeminsu.core.data.repository.payment.DefaultOwnerPaymentRepository
import com.ssafy.cafeminsu.core.data.repository.payment.OwnerPaymentRepository
import com.ssafy.cafeminsu.core.data.repository.store.DefaultOwnerStoreRepository
import com.ssafy.cafeminsu.core.data.repository.store.DefaultStoreRepository
import com.ssafy.cafeminsu.core.data.repository.store.OwnerStoreRepository
import com.ssafy.cafeminsu.core.data.repository.store.StoreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindAuthRepository(
        authRepository: DefaultAuthRepository,
    ): AuthRepository

    @Binds
    internal abstract fun bindStoreRepository(
        storeRepository: DefaultStoreRepository,
    ): StoreRepository

    @Binds
    internal abstract fun bindOwnerStoreRepository(
        ownerStoreRepository: DefaultOwnerStoreRepository,
    ): OwnerStoreRepository

    @Binds
    internal abstract fun bindMenuRepository(
        menuRepository: OfflineFirstMenuRepository,
    ): MenuRepository

    @Binds
    internal abstract fun bindOwnerMenuRepository(
        ownerMenuRepository: DefaultOwnerMenuRepository,
    ): OwnerMenuRepository

    @Binds
    internal abstract fun bindOrderRepository(
        orderRepository: DefaultOrderRepository,
    ): OrderRepository

    @Binds
    internal abstract fun bindOwnerOrderRepository(
        ownerOrderRepository: DefaultOwnerOrderRepository,
    ): OwnerOrderRepository

    @Binds
    internal abstract fun bindOwnerPaymentRepository(
        ownerPaymentRepository: DefaultOwnerPaymentRepository,
    ): OwnerPaymentRepository

    @Binds
    internal abstract fun bindNotificationRepository(
        notificationRepository: DefaultNotificationRepository,
    ): NotificationRepository

    @Binds
    internal abstract fun bindCartRepository(
        cartRepository: DefaultCartRepository,
    ): CartRepository
}
