package com.cafeminsu.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.model.UserRole
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
                    ownerAuthProvider = FakeOwnerAuthProvider(),
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
                    ownerAuthProvider = FakeOwnerAuthProvider(),
                    splashDelayMillis = 0,
                )
            }
        }

        composeRule.onNodeWithText("카페민수").assertIsDisplayed()
    }

    @Test
    fun ownerLoginLinkRoutesToOwnerLoginScreen() {
        composeRule.setContent {
            CafeTheme {
                AppNavHost(
                    sessionRepository = FakeSessionRepository(AuthState.Guest),
                    ownerAuthProvider = FakeOwnerAuthProvider(),
                    splashDelayMillis = 0,
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("점주 로그인").performClick()

        composeRule.onNodeWithText("매장 관리자 로그인").assertIsDisplayed()
    }

    @Test
    fun ownerAuthenticatedSplashRoutesToOwnerShell() {
        val ownerAuthState = AuthState.Authenticated(
            user = UserProfile(
                id = "owner-user",
                displayName = "강남점 점주",
                phoneLast4 = null,
            ),
            role = UserRole.Owner,
        )

        composeRule.setContent {
            CafeTheme {
                AppNavHost(
                    sessionRepository = FakeSessionRepository(ownerAuthState),
                    ownerAuthProvider = FakeOwnerAuthProvider(),
                    splashDelayMillis = 0,
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("대시보드").assertIsDisplayed()
        composeRule.onNodeWithText("주문").assertIsDisplayed()
        composeRule.onNodeWithText("메뉴").assertIsDisplayed()
        composeRule.onNodeWithText("매출").assertIsDisplayed()
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

private class FakeOwnerAuthProvider : OwnerAuthProvider {
    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> =
        AppResult.Success(
            OwnerProfile(
                id = "owner-demo",
                storeId = "store-gangnam",
                storeName = "강남점",
                loginId = loginId,
                isStoreOpen = true,
            ),
        )

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> =
        AppResult.Success(
            OwnerProfile(
                id = "owner-demo",
                storeId = "store-gangnam",
                storeName = "강남점",
                loginId = "owner",
                isStoreOpen = open,
            ),
        )
}
