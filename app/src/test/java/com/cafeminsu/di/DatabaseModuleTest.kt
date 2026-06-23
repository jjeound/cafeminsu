package com.cafeminsu.di

import com.cafeminsu.data.local.db.CafeDatabase
import com.cafeminsu.data.local.notification.NotificationDao
import com.cafeminsu.data.local.notification.NotificationLocalDataSource
import com.cafeminsu.data.local.order.OrderHistoryDao
import com.cafeminsu.data.local.order.OrderHistoryLocalDataSource
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 순수 JVM 리플렉션으로 새 캐시 의존성 배선(반환 타입)을 검증한다. Room 인스턴스(안드로이드 컨텍스트)
 * 없이도 @Provides 시그니처가 인터페이스 계약을 지키는지 확인한다(RepositoryModuleTest 와 동일 방식).
 */
class DatabaseModuleTest {
    @Test
    fun providesNotificationDaoFromDatabase() {
        val method = DatabaseModule::class.java.getDeclaredMethod(
            "provideNotificationDao",
            CafeDatabase::class.java,
        )

        assertEquals(NotificationDao::class.java, method.returnType)
    }

    @Test
    fun providesNotificationLocalDataSourceAsInterface() {
        val method = DatabaseModule::class.java.getDeclaredMethod(
            "provideNotificationLocalDataSource",
            NotificationDao::class.java,
        )

        assertEquals(NotificationLocalDataSource::class.java, method.returnType)
    }

    @Test
    fun providesOrderHistoryDaoFromDatabase() {
        val method = DatabaseModule::class.java.getDeclaredMethod(
            "provideOrderHistoryDao",
            CafeDatabase::class.java,
        )

        assertEquals(OrderHistoryDao::class.java, method.returnType)
    }

    @Test
    fun providesOrderHistoryLocalDataSourceAsInterface() {
        val method = DatabaseModule::class.java.getDeclaredMethod(
            "provideOrderHistoryLocalDataSource",
            OrderHistoryDao::class.java,
            Moshi::class.java,
        )

        assertEquals(OrderHistoryLocalDataSource::class.java, method.returnType)
    }
}
