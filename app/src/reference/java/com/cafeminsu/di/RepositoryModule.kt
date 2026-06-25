package com.cafeminsu.di

import com.cafeminsu.data.payment.KakaoPayPgClient
import com.cafeminsu.data.payment.KakaoPayRedirectBridge
import com.cafeminsu.data.payment.MockPgClient
import com.cafeminsu.data.payment.NoOpKakaoPayRedirectBridge
import com.cafeminsu.data.payment.PgClient
import com.cafeminsu.data.platform.AndroidMenuImageReader
import com.cafeminsu.data.platform.MenuImageReader
import com.cafeminsu.data.platform.RealKakaoPayRedirectBridge
import com.cafeminsu.data.repository.MockCartRepository
import com.cafeminsu.data.repository.MockCouponRepository
import com.cafeminsu.data.repository.MockFcmTokenRepository
import com.cafeminsu.data.repository.MockGiftRepository
import com.cafeminsu.data.repository.MockMenuRepository
import com.cafeminsu.data.repository.MockNotificationRepository
import com.cafeminsu.data.repository.MockOrderRepository
import com.cafeminsu.data.repository.MockOwnerMenuRepository
import com.cafeminsu.data.repository.MockOwnerOrderRepository
import com.cafeminsu.data.repository.MockPaymentRepository
import com.cafeminsu.data.repository.MockRecommendationRepository
import com.cafeminsu.data.repository.MockRewardRepository
import com.cafeminsu.data.repository.MockSalesRepository
import com.cafeminsu.data.repository.MockSessionRepository
import com.cafeminsu.data.repository.MockStoreRepository
import com.cafeminsu.data.repository.RealFcmTokenRepository
import com.cafeminsu.data.repository.RealGiftRepository
import com.cafeminsu.data.repository.RealMenuRepository
import com.cafeminsu.data.repository.RealNotificationRepository
import com.cafeminsu.data.repository.RealOrderRepository
import com.cafeminsu.data.repository.RealOwnerMenuRepository
import com.cafeminsu.data.repository.RealOwnerOrderRepository
import com.cafeminsu.data.repository.RealPaymentRepository
import com.cafeminsu.data.repository.RealRecommendationRepository
import com.cafeminsu.data.repository.RealRewardRepository
import com.cafeminsu.data.repository.RealSessionRepository
import com.cafeminsu.data.repository.RealStoreRepository
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.CouponRepository
import com.cafeminsu.domain.repository.FcmTokenRepository
import com.cafeminsu.domain.repository.GiftRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.repository.NotificationRepository
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.OwnerMenuRepository
import com.cafeminsu.domain.repository.OwnerOrderRepository
import com.cafeminsu.domain.repository.PaymentRepository
import com.cafeminsu.domain.repository.RecommendationRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SalesRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.domain.repository.StoreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCartRepository(repository: MockCartRepository): CartRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(repository: MockSalesRepository): SalesRepository

    @Binds
    @Singleton
    abstract fun bindCouponRepository(repository: MockCouponRepository): CouponRepository

    // 점주 메뉴 이미지 업로드를 위한 로컬 이미지 리더(프레임워크 의존 — Android ContentResolver).
    @Binds
    @Singleton
    abstract fun bindMenuImageReader(reader: AndroidMenuImageReader): MenuImageReader

    companion object {
        @Provides
        @Singleton
        fun providePgClient(
            real: Provider<KakaoPayPgClient>,
            mock: Provider<MockPgClient>,
        ): PgClient =
            if (com.cafeminsu.BuildConfig.KAKAOPAY_ENABLED) {
                real.get()
            } else {
                mock.get()
            }

        @Provides
        @Singleton
        fun provideKakaoPayRedirectBridge(
            real: Provider<RealKakaoPayRedirectBridge>,
            noOp: Provider<NoOpKakaoPayRedirectBridge>,
        ): KakaoPayRedirectBridge =
            if (com.cafeminsu.BuildConfig.KAKAOPAY_ENABLED) {
                real.get()
            } else {
                noOp.get()
            }

        @Provides
        @Singleton
        fun provideNotificationRepository(
            realRepository: Provider<RealNotificationRepository>,
            mockRepository: Provider<MockNotificationRepository>,
        ): NotificationRepository =
            selectNotificationRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideGiftRepository(
            realRepository: Provider<RealGiftRepository>,
            mockRepository: Provider<MockGiftRepository>,
        ): GiftRepository =
            selectGiftRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideMenuRepository(
            realRepository: Provider<RealMenuRepository>,
            mockRepository: Provider<MockMenuRepository>,
        ): MenuRepository =
            selectMenuRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideStoreRepository(
            realRepository: Provider<RealStoreRepository>,
            mockRepository: Provider<MockStoreRepository>,
        ): StoreRepository =
            selectStoreRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideOrderRepository(
            realRepository: Provider<RealOrderRepository>,
            mockRepository: Provider<MockOrderRepository>,
        ): OrderRepository =
            selectOrderRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideOwnerOrderRepository(
            realRepository: Provider<RealOwnerOrderRepository>,
            mockRepository: Provider<MockOwnerOrderRepository>,
        ): OwnerOrderRepository =
            selectOwnerOrderRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideOwnerMenuRepository(
            realRepository: Provider<RealOwnerMenuRepository>,
            mockRepository: Provider<MockOwnerMenuRepository>,
        ): OwnerMenuRepository =
            selectOwnerMenuRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun providePaymentRepository(
            realRepository: Provider<RealPaymentRepository>,
            mockRepository: Provider<MockPaymentRepository>,
        ): PaymentRepository =
            selectPaymentRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideRecommendationRepository(
            realRepository: Provider<RealRecommendationRepository>,
            mockRepository: Provider<MockRecommendationRepository>,
        ): RecommendationRepository =
            selectRecommendationRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideRewardRepository(
            realRepository: Provider<RealRewardRepository>,
            mockRepository: Provider<MockRewardRepository>,
        ): RewardRepository =
            selectRewardRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideFcmTokenRepository(
            realRepository: Provider<RealFcmTokenRepository>,
            mockRepository: Provider<MockFcmTokenRepository>,
        ): FcmTokenRepository =
            selectFcmTokenRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )

        @Provides
        @Singleton
        fun provideSessionRepository(
            realRepository: Provider<RealSessionRepository>,
            mockRepository: Provider<MockSessionRepository>,
        ): SessionRepository =
            selectSessionRepository(
                baseUrl = com.cafeminsu.BuildConfig.BASE_URL,
                realFactory = { realRepository.get() },
                mockFactory = { mockRepository.get() },
            )
    }
}

internal fun selectSessionRepository(
    baseUrl: String,
    realFactory: () -> SessionRepository,
    mockFactory: () -> SessionRepository,
): SessionRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectStoreRepository(
    baseUrl: String,
    realFactory: () -> StoreRepository,
    mockFactory: () -> StoreRepository,
): StoreRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectMenuRepository(
    baseUrl: String,
    realFactory: () -> MenuRepository,
    mockFactory: () -> MenuRepository,
): MenuRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectOrderRepository(
    baseUrl: String,
    realFactory: () -> OrderRepository,
    mockFactory: () -> OrderRepository,
): OrderRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectOwnerOrderRepository(
    baseUrl: String,
    realFactory: () -> OwnerOrderRepository,
    mockFactory: () -> OwnerOrderRepository,
): OwnerOrderRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectOwnerMenuRepository(
    baseUrl: String,
    realFactory: () -> OwnerMenuRepository,
    mockFactory: () -> OwnerMenuRepository,
): OwnerMenuRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectPaymentRepository(
    baseUrl: String,
    realFactory: () -> PaymentRepository,
    mockFactory: () -> PaymentRepository,
): PaymentRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectRewardRepository(
    baseUrl: String,
    realFactory: () -> RewardRepository,
    mockFactory: () -> RewardRepository,
): RewardRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectRecommendationRepository(
    baseUrl: String,
    realFactory: () -> RecommendationRepository,
    mockFactory: () -> RecommendationRepository,
): RecommendationRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectGiftRepository(
    baseUrl: String,
    realFactory: () -> GiftRepository,
    mockFactory: () -> GiftRepository,
): GiftRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectNotificationRepository(
    baseUrl: String,
    realFactory: () -> NotificationRepository,
    mockFactory: () -> NotificationRepository,
): NotificationRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }

internal fun selectFcmTokenRepository(
    baseUrl: String,
    realFactory: () -> FcmTokenRepository,
    mockFactory: () -> FcmTokenRepository,
): FcmTokenRepository =
    if (baseUrl.isNotBlank()) {
        realFactory()
    } else {
        mockFactory()
    }
