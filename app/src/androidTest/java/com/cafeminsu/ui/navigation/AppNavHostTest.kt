package com.cafeminsu.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.theme.CafeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startsAtSplashThenRoutesGuestToLogin() {
        composeRule.setContent {
            CafeTheme {
                AppNavHost(
                    sessionRepository = FakeSessionRepository(AuthState.Guest),
                    splashDelayMillis = 0,
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("카카오 로그인").assertIsDisplayed()
    }

    @Test
    fun splashScreenIsStartDestination() {
        composeRule.setContent {
            CafeTheme {
                AppNavHost(
                    sessionRepository = FakeSessionRepository(AuthState.Unknown),
                    splashDelayMillis = 0,
                )
            }
        }

        composeRule.onNodeWithText("카페민수").assertIsDisplayed()
    }
}

private class FakeSessionRepository(
    initialAuthState: AuthState,
) : SessionRepository {
    private val authState = MutableStateFlow(initialAuthState)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}
