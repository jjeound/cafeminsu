package com.cafeminsu.data.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMenuImageReaderTest {
    @Test
    fun returnsNullForUnreadableUriInsteadOfThrowing() = runTest {
        // 존재하지 않는 content:// URI 는 예외 대신 null 로 안전하게 폴백해야 한다(크래시 금지).
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reader = AndroidMenuImageReader(context)

        assertNull(reader.read("content://com.cafeminsu.invalid/does-not-exist"))
    }
}
