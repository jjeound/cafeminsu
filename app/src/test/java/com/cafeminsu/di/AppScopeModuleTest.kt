package com.cafeminsu.di

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppScopeModuleTest {
    @Test
    fun createApplicationScopeIsActiveAndUsesProvidedDispatcher() {
        val dispatcher = StandardTestDispatcher()

        val scope = createApplicationScope(dispatcher)

        assertTrue(scope.isActive)
        assertEquals(dispatcher, scope.coroutineContext[ContinuationInterceptor])
    }

    @Test
    fun createApplicationScopeUsesSupervisorJob() {
        val scope = createApplicationScope(StandardTestDispatcher())

        val job = scope.coroutineContext[Job]

        assertNotNull(job)
        // SupervisorJob: 자식 실패가 형제/스코프를 취소하지 않는 잡이어야 한다.
        assertTrue(job.toString().contains("Supervisor"))
    }
}
