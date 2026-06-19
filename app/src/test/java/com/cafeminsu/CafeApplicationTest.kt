package com.cafeminsu

import org.junit.Assert.assertEquals
import org.junit.Test

class CafeApplicationTest {
    @Test
    fun cafeApplicationKeepsCafeMinsuPackage() {
        assertEquals("com.cafeminsu.CafeApplication", CafeApplication::class.java.name)
    }
}
