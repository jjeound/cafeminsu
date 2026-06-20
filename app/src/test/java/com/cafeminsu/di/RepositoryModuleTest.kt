package com.cafeminsu.di

import com.cafeminsu.data.repository.MockCartRepository
import com.cafeminsu.data.repository.MockCouponRepository
import com.cafeminsu.data.repository.MockGiftRepository
import com.cafeminsu.data.repository.MockMenuRepository
import com.cafeminsu.data.repository.MockNotificationRepository
import com.cafeminsu.data.repository.MockOrderRepository
import com.cafeminsu.data.repository.MockOwnerMenuRepository
import com.cafeminsu.data.repository.MockOwnerOrderRepository
import com.cafeminsu.data.repository.MockPaymentRepository
import com.cafeminsu.data.repository.MockRewardRepository
import com.cafeminsu.data.repository.MockSessionRepository
import com.cafeminsu.data.repository.MockStoreRepository
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.CouponRepository
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
        assertBinding("bindOrderRepository", OrderRepository::class.java, MockOrderRepository::class.java)
        assertBinding(
            "bindOwnerOrderRepository",
            OwnerOrderRepository::class.java,
            MockOwnerOrderRepository::class.java,
        )
        assertBinding(
            "bindOwnerMenuRepository",
            OwnerMenuRepository::class.java,
            MockOwnerMenuRepository::class.java,
        )
        assertBinding("bindPaymentRepository", PaymentRepository::class.java, MockPaymentRepository::class.java)
        assertBinding("bindRewardRepository", RewardRepository::class.java, MockRewardRepository::class.java)
        assertBinding("bindCouponRepository", CouponRepository::class.java, MockCouponRepository::class.java)
        assertBinding("bindGiftRepository", GiftRepository::class.java, MockGiftRepository::class.java)
        assertBinding(
            "bindNotificationRepository",
            NotificationRepository::class.java,
            MockNotificationRepository::class.java,
        )
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
