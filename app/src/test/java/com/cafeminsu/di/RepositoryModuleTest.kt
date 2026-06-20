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
    fun repositoryModuleBindsAllRepositoryContractsToMockSingletons() {
        assertBinding("bindMenuRepository", MenuRepository::class.java, MockMenuRepository::class.java)
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
        assertBinding("bindSessionRepository", SessionRepository::class.java, MockSessionRepository::class.java)
        assertBinding("bindStoreRepository", StoreRepository::class.java, MockStoreRepository::class.java)
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
