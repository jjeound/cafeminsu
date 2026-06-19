package com.cafeminsu.di

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

class DispatcherModuleTest {
    @Test
    fun dispatcherModuleProvidesIoDispatcher() {
        assertSame(Dispatchers.IO, DispatcherModule.provideIo())
    }
}
