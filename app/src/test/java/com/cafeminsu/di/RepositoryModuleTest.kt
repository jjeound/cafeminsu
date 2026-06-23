package com.cafeminsu.di

import com.cafeminsu.data.repository.MockCartRepository
import com.cafeminsu.data.repository.MockCouponRepository
import com.cafeminsu.data.repository.MockGiftRepository
import com.cafeminsu.data.repository.MockMenuRepository
import com.cafeminsu.data.repository.MockNotificationRepository
import com.cafeminsu.data.repository.MockOrderRepository
import com.cafeminsu.data.repository.MockOwnerMenuRepository
import com.cafeminsu.data.repository.MockOwnerOrderRepository
import com.cafeminsu.data.repository.MockFcmTokenRepository
import com.cafeminsu.data.repository.MockSessionRepository
import com.cafeminsu.data.repository.MockStoreRepository
import com.cafeminsu.data.repository.RealFcmTokenRepository
import com.cafeminsu.data.repository.RealGiftRepository
import com.cafeminsu.data.repository.RealNotificationRepository
import com.cafeminsu.data.repository.RealOwnerOrderRepository
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
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.domain.repository.StoreRepository
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryModuleTest {
    @Test
    fun repositoryModuleBindsUnchangedRepositoryContractsToMockSingletons() {
        assertBinding("bindCartRepository", CartRepository::class.java, MockCartRepository::class.java)
        assertBinding(
            "bindOwnerMenuRepository",
            OwnerMenuRepository::class.java,
            MockOwnerMenuRepository::class.java,
        )
        assertBinding("bindCouponRepository", CouponRepository::class.java, MockCouponRepository::class.java)
    }

    @Test
    fun blankBaseUrlSelectsMockSessionRepositoryFallback() {
        val realRepository = FakeSessionRepository()
        val mockRepository = FakeSessionRepository()

        val selected = selectSessionRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealSessionRepository() {
        val realRepository = FakeSessionRepository()
        val mockRepository = FakeSessionRepository()

        val selected = selectSessionRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun blankBaseUrlSelectsMockOrderRepositoryFallback() {
        val realRepository = FakeOrderRepository()
        val mockRepository = FakeOrderRepository()

        val selected = selectOrderRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealOrderRepository() {
        val realRepository = FakeOrderRepository()
        val mockRepository = FakeOrderRepository()

        val selected = selectOrderRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun blankBaseUrlSelectsMockOwnerOrderRepositoryFallback() {
        val realRepository = FakeOwnerOrderRepository()
        val mockRepository = FakeOwnerOrderRepository()

        val selected = selectOwnerOrderRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealOwnerOrderRepository() {
        val realRepository = FakeOwnerOrderRepository()
        val mockRepository = FakeOwnerOrderRepository()

        val selected = selectOwnerOrderRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun repositoryModuleProvidesOwnerOrderRepositoryWithRealAndMockProviders() {
        val method = RepositoryModule.Companion::class.java.getDeclaredMethod(
            "provideOwnerOrderRepository",
            javax.inject.Provider::class.java,
            javax.inject.Provider::class.java,
        )

        assertEquals(OwnerOrderRepository::class.java, method.returnType)
        assertEquals(RealOwnerOrderRepository::class.java, method.genericParameterTypes.first().providerArgument())
        assertEquals(MockOwnerOrderRepository::class.java, method.genericParameterTypes.last().providerArgument())
    }

    @Test
    fun blankBaseUrlSelectsMockPaymentRepositoryFallback() {
        val realRepository = FakePaymentRepository()
        val mockRepository = FakePaymentRepository()

        val selected = selectPaymentRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealPaymentRepository() {
        val realRepository = FakePaymentRepository()
        val mockRepository = FakePaymentRepository()

        val selected = selectPaymentRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun blankBaseUrlSelectsMockRewardRepositoryFallback() {
        val realRepository = FakeRewardRepository()
        val mockRepository = FakeRewardRepository()

        val selected = selectRewardRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealRewardRepository() {
        val realRepository = FakeRewardRepository()
        val mockRepository = FakeRewardRepository()

        val selected = selectRewardRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun blankBaseUrlSelectsMockGiftRepositoryFallback() {
        val realRepository = FakeGiftRepository()
        val mockRepository = FakeGiftRepository()

        val selected = selectGiftRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealGiftRepository() {
        val realRepository = FakeGiftRepository()
        val mockRepository = FakeGiftRepository()

        val selected = selectGiftRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun repositoryModuleProvidesGiftRepositoryWithRealAndMockProviders() {
        val method = RepositoryModule.Companion::class.java.getDeclaredMethod(
            "provideGiftRepository",
            javax.inject.Provider::class.java,
            javax.inject.Provider::class.java,
        )

        assertEquals(GiftRepository::class.java, method.returnType)
        assertEquals(RealGiftRepository::class.java, method.genericParameterTypes.first().providerArgument())
        assertEquals(MockGiftRepository::class.java, method.genericParameterTypes.last().providerArgument())
    }

    @Test
    fun blankBaseUrlSelectsMockNotificationRepositoryFallback() {
        val realRepository = FakeNotificationRepository()
        val mockRepository = FakeNotificationRepository()

        val selected = selectNotificationRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealNotificationRepository() {
        val realRepository = FakeNotificationRepository()
        val mockRepository = FakeNotificationRepository()

        val selected = selectNotificationRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun repositoryModuleProvidesNotificationRepositoryWithRealAndMockProviders() {
        val method = RepositoryModule.Companion::class.java.getDeclaredMethod(
            "provideNotificationRepository",
            javax.inject.Provider::class.java,
            javax.inject.Provider::class.java,
        )

        assertEquals(NotificationRepository::class.java, method.returnType)
        assertEquals(RealNotificationRepository::class.java, method.genericParameterTypes.first().providerArgument())
        assertEquals(MockNotificationRepository::class.java, method.genericParameterTypes.last().providerArgument())
    }

    @Test
    fun blankBaseUrlSelectsMockFcmTokenRepositoryFallback() {
        val realRepository = FakeFcmTokenRepository()
        val mockRepository = FakeFcmTokenRepository()

        val selected = selectFcmTokenRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealFcmTokenRepository() {
        val realRepository = FakeFcmTokenRepository()
        val mockRepository = FakeFcmTokenRepository()

        val selected = selectFcmTokenRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun repositoryModuleProvidesFcmTokenRepositoryWithRealAndMockProviders() {
        val method = RepositoryModule.Companion::class.java.getDeclaredMethod(
            "provideFcmTokenRepository",
            javax.inject.Provider::class.java,
            javax.inject.Provider::class.java,
        )

        assertEquals(FcmTokenRepository::class.java, method.returnType)
        assertEquals(RealFcmTokenRepository::class.java, method.genericParameterTypes.first().providerArgument())
        assertEquals(MockFcmTokenRepository::class.java, method.genericParameterTypes.last().providerArgument())
    }

    @Test
    fun blankBaseUrlSelectsMockStoreRepositoryFallback() {
        val realRepository = FakeStoreRepository()
        val mockRepository = FakeStoreRepository()

        val selected = selectStoreRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealStoreRepository() {
        val realRepository = FakeStoreRepository()
        val mockRepository = FakeStoreRepository()

        val selected = selectStoreRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    @Test
    fun blankBaseUrlSelectsMockMenuRepositoryFallback() {
        val realRepository = FakeMenuRepository()
        val mockRepository = FakeMenuRepository()

        val selected = selectMenuRepository(
            baseUrl = "",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(mockRepository, selected)
    }

    @Test
    fun configuredBaseUrlSelectsRealMenuRepository() {
        val realRepository = FakeMenuRepository()
        val mockRepository = FakeMenuRepository()

        val selected = selectMenuRepository(
            baseUrl = "https://cafeminsu.example/",
            realFactory = { realRepository },
            mockFactory = { mockRepository },
        )

        assertEquals(realRepository, selected)
    }

    private fun assertBinding(
        methodName: String,
        repositoryType: Class<*>,
        implementationType: Class<*>,
    ) {
        val method = RepositoryModule::class.java.getDeclaredMethod(methodName, implementationType)

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(repositoryType, method.returnType)
    }

    private fun java.lang.reflect.Type.providerArgument(): Class<*> =
        (this as java.lang.reflect.ParameterizedType).actualTypeArguments.single() as Class<*>
}

private class FakeSessionRepository : SessionRepository {
    override fun observeAuthState(): kotlinx.coroutines.flow.Flow<com.cafeminsu.domain.model.AuthState> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.domain.model.AuthState.Guest)

    override suspend fun refreshOnce(): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.AuthState> =
        com.cafeminsu.core.AppResult.Success(com.cafeminsu.domain.model.AuthState.Guest)

    override suspend fun clearSession(): com.cafeminsu.core.AppResult<Unit> =
        com.cafeminsu.core.AppResult.Success(Unit)
}

private class FakeStoreRepository : StoreRepository {
    override fun observeNearbyStores(
        query: String?,
    ): kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.Store>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override suspend fun getStore(storeId: String): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Store> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

    override suspend fun selectStore(storeId: String): com.cafeminsu.core.AppResult<Unit> =
        com.cafeminsu.core.AppResult.Success(Unit)

    override fun observeSelectedStore(): kotlinx.coroutines.flow.Flow<com.cafeminsu.domain.model.Store?> =
        kotlinx.coroutines.flow.flowOf(null)
}

private class FakeMenuRepository : MenuRepository {
    override fun observeCategories():
        kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.MenuCategory>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override fun observeMenus(
        categoryId: String?,
    ): kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.MenuItem>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override suspend fun getMenu(
        menuItemId: String,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.MenuItem> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

    override suspend fun refreshMenus(): com.cafeminsu.core.AppResult<Unit> =
        com.cafeminsu.core.AppResult.Success(Unit)
}

private class FakeOrderRepository : OrderRepository {
    override suspend fun createOrderFromCart(
        cart: com.cafeminsu.domain.model.Cart,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Order> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

    override fun observeOrder(
        orderId: String,
    ): kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Order>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound))

    override fun observeOrderHistory():
        kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.Order>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))
}

private class FakePaymentRepository : PaymentRepository {
    override suspend fun pay(
        request: com.cafeminsu.domain.model.PaymentRequest,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.PaymentResult> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

    override suspend fun getPaymentStatus(
        orderId: String,
        idempotencyKey: String,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.PaymentResult> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)
}

private class FakeRewardRepository : RewardRepository {
    override fun observeStampCard():
        kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.StampCard>> =
        kotlinx.coroutines.flow.flowOf(
            com.cafeminsu.core.AppResult.Success(
                com.cafeminsu.domain.model.StampCard(
                    userId = "user",
                    currentCount = 0,
                    goalCount = 10,
                    history = emptyList(),
                ),
            ),
        )

    override suspend fun grantStampsForPaidOrder(
        orderId: String,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.StampCard> =
        com.cafeminsu.core.AppResult.Success(
            com.cafeminsu.domain.model.StampCard(
                userId = "user",
                currentCount = 1,
                goalCount = 10,
                history = emptyList(),
            ),
        )

    override fun observeGifticons():
        kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.Gifticon>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override suspend fun getGifticon(
        id: String,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Gifticon> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

    override suspend fun markGifticonUsed(
        id: String,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Gifticon> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)
}

private class FakeGiftRepository : GiftRepository {
    override suspend fun sendGift(
        request: com.cafeminsu.domain.model.GiftSendRequest,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.GiftSendResult> =
        com.cafeminsu.core.AppResult.Success(
            com.cafeminsu.domain.model.GiftSendResult(
                giftId = "gift",
                sentAtMillis = 0L,
            ),
        )
}

private class FakeNotificationRepository : NotificationRepository {
    override fun observeNotifications():
        kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.AppNotification>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override suspend fun markAllRead(): com.cafeminsu.core.AppResult<Unit> =
        com.cafeminsu.core.AppResult.Success(Unit)
}

private class FakeFcmTokenRepository : FcmTokenRepository {
    override suspend fun register(token: String): com.cafeminsu.core.AppResult<Unit> =
        com.cafeminsu.core.AppResult.Success(Unit)
}

private class FakeOwnerOrderRepository : OwnerOrderRepository {
    override fun observeIncomingOrders(
        filter: com.cafeminsu.domain.model.OrderStatus?,
    ): kotlinx.coroutines.flow.Flow<com.cafeminsu.core.AppResult<List<com.cafeminsu.domain.model.Order>>> =
        kotlinx.coroutines.flow.flowOf(com.cafeminsu.core.AppResult.Success(emptyList()))

    override suspend fun advanceStatus(
        orderId: String,
        to: com.cafeminsu.domain.model.OrderStatus,
    ): com.cafeminsu.core.AppResult<com.cafeminsu.domain.model.Order> =
        com.cafeminsu.core.AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)
}
