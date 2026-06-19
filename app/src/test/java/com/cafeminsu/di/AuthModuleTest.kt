package com.cafeminsu.di

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import org.junit.Assert.assertSame
import org.junit.Test

class AuthModuleTest {
    @Test
    fun emptyKakaoNativeAppKeySelectsMockLoginProvider() {
        val realProvider = FakeLoginProvider()
        val mockProvider = FakeLoginProvider()

        val selected = selectLoginProvider(
            kakaoNativeAppKey = "",
            realFactory = { realProvider },
            mockFactory = { mockProvider },
        )

        assertSame(mockProvider, selected)
    }

    @Test
    fun nonBlankKakaoNativeAppKeySelectsRealLoginProvider() {
        val realProvider = FakeLoginProvider()
        val mockProvider = FakeLoginProvider()

        val selected = selectLoginProvider(
            kakaoNativeAppKey = "native-key",
            realFactory = { realProvider },
            mockFactory = { mockProvider },
        )

        assertSame(realProvider, selected)
    }
}

private class FakeLoginProvider : LoginProvider {
    override suspend fun login(): AppResult<AuthState> =
        AppResult.Success(AuthState.Guest)

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)
}
