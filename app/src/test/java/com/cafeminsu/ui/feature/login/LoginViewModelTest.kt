package com.cafeminsu.ui.feature.login

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = LoginMainDispatcherRule()

    @Test
    fun loginSuccessProducesAuthenticatedStateAndNavigateHomeEvent() = runTest {
        val viewModel = LoginViewModel(
            sessionRepository = FakeLoginSessionRepository(
                loginResult = AppResult.Success(authenticatedUser()),
            ),
        )

        viewModel.events.test {
            viewModel.onKakaoLoginClick()

            assertEquals(LoginEvent.NavigateHome, awaitItem())
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(null, viewModel.uiState.value.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailureProducesErrorStateAndSnackbarEvent() = runTest {
        val viewModel = LoginViewModel(
            sessionRepository = FakeLoginSessionRepository(
                loginResult = AppResult.Failure(DomainError.Network),
            ),
        )

        viewModel.events.test {
            viewModel.onKakaoLoginClick()

            val event = awaitItem()
            assertTrue(event is LoginEvent.ShowSnackbar)
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", (event as LoginEvent.ShowSnackbar).message)
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newUserLoginProducesNavigateSignupEvent() = runTest {
        val viewModel = LoginViewModel(
            sessionRepository = FakeLoginSessionRepository(
                loginResult = AppResult.Success(newUser()),
            ),
        )

        viewModel.events.test {
            viewModel.onKakaoLoginClick()

            assertEquals(LoginEvent.NavigateSignup, awaitItem())
            assertTrue(viewModel.uiState.value.isAuthenticated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginClickCallsSessionLoginOnce() = runTest {
        val sessionRepository = FakeLoginSessionRepository(
            loginResult = AppResult.Success(authenticatedUser()),
        )
        val viewModel = LoginViewModel(sessionRepository)

        viewModel.onKakaoLoginClick()

        assertEquals(1, sessionRepository.loginCalls)
    }

    private fun authenticatedUser(): AuthState =
        AuthState.Authenticated(
            UserProfile(
                id = "user-1",
                displayName = "민수",
                phoneLast4 = "1234",
            ),
        )

    private fun newUser(): AuthState =
        AuthState.Authenticated(
            user = UserProfile(
                id = "user-2",
                displayName = "카페민수 사용자",
                phoneLast4 = null,
            ),
            isNewUser = true,
        )
}

private class FakeLoginSessionRepository(
    private val loginResult: AppResult<AuthState>,
) : SessionRepository {
    private val authState = MutableStateFlow<AuthState>(AuthState.Guest)
    var loginCalls: Int = 0
        private set

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(authState.value)

    override suspend fun login(): AppResult<AuthState> {
        loginCalls += 1
        if (loginResult is AppResult.Success) {
            authState.value = loginResult.data
        }
        return loginResult
    }

    override suspend fun clearSession(): AppResult<Unit> {
        authState.value = AuthState.Guest
        return AppResult.Success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
