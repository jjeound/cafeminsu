package com.cafeminsu.ui.feature.owner.login

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class OwnerLoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = OwnerLoginMainDispatcherRule()

    @Test
    fun loginSuccessProducesAuthenticatedStateAndNavigateOwnerHomeEvent() = runTest {
        val viewModel = OwnerLoginViewModel(
            ownerAuthProvider = FakeOwnerAuthProvider(
                loginResult = AppResult.Success(ownerProfile()),
            ),
        )

        viewModel.events.test {
            viewModel.login(loginId = "owner", password = "owner-secret")

            assertEquals(OwnerLoginEvent.NavigateOwnerHome, awaitItem())
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.toString().contains("owner-secret"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailureProducesErrorStateAndSnackbarEventWithoutPassword() = runTest {
        val viewModel = OwnerLoginViewModel(
            ownerAuthProvider = FakeOwnerAuthProvider(
                loginResult = AppResult.Failure(DomainError.Unauthorized),
            ),
        )

        viewModel.events.test {
            viewModel.login(loginId = "owner", password = "owner-secret")

            val event = awaitItem()
            assertTrue(event is OwnerLoginEvent.ShowSnackbar)
            assertEquals("아이디 또는 비밀번호를 확인해 주세요", (event as OwnerLoginEvent.ShowSnackbar).message)
            assertEquals("아이디 또는 비밀번호를 확인해 주세요", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.toString().contains("owner-secret"))
            assertFalse(event.toString().contains("owner-secret"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginClickCallsProviderWithTransientPasswordOnly() = runTest {
        val ownerAuthProvider = FakeOwnerAuthProvider(
            loginResult = AppResult.Success(ownerProfile()),
        )
        val viewModel = OwnerLoginViewModel(ownerAuthProvider)

        viewModel.login(loginId = "owner", password = "owner-secret")

        assertEquals(1, ownerAuthProvider.loginCalls)
        assertEquals("owner", ownerAuthProvider.lastLoginId)
        assertEquals("owner-secret", ownerAuthProvider.lastPassword)
        assertFalse(viewModel.uiState.value.toString().contains("owner-secret"))
    }

    private fun ownerProfile(): OwnerProfile =
        OwnerProfile(
            id = "owner-demo",
            storeId = "store-gangnam",
            storeName = "강남점",
            loginId = "owner",
            isStoreOpen = true,
        )
}

private class FakeOwnerAuthProvider(
    private val loginResult: AppResult<OwnerProfile>,
) : OwnerAuthProvider {
    var loginCalls: Int = 0
        private set
    var lastLoginId: String? = null
        private set
    var lastPassword: String? = null
        private set

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> {
        loginCalls += 1
        lastLoginId = loginId
        lastPassword = password
        return loginResult
    }

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> =
        loginResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerLoginMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
