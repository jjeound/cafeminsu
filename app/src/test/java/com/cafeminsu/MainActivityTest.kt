package com.cafeminsu

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun mainActivityKeepsCafeMinsuPackage() {
        assertEquals("com.cafeminsu.MainActivity", MainActivity::class.java.name)
    }
}
