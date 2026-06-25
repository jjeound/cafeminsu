package com.cafeminsu.data.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidLocationProviderTest {
    @Test
    fun returnsNullWhenLocationPermissionNotGranted() = runTest {
        // 인스트루먼테이션 컨텍스트는 위치 권한이 부여돼 있지 않으므로 안전하게 null 을 돌려준다(예외 없음).
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocationProvider(context)

        assertNull(provider.currentLatLng())
    }
}
