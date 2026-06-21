package com.cafeminsu.ui.feature.signup

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
class SignupViewModelTest {
    @get:Rule
    val mainDispatcherRule = SignupMainDispatcherRule()

    @Test
    fun validNicknameEnablesSubmitWithoutError() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("민수")

        val state = viewModel.uiState.value
        assertEquals("민수", state.nickname)
        assertTrue(state.isNicknameValid)
        assertTrue(state.canSubmit)
        assertEquals(null, state.errorMessage)
        assertEquals(2, state.charCount)
    }

    @Test
    fun shortNicknameShowsRuleErrorAndBlocksSubmit() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("민")

        val state = viewModel.uiState.value
        assertFalse(state.isNicknameValid)
        assertFalse(state.canSubmit)
        assertEquals("한글·영문·숫자 2~10자로 입력해주세요", state.errorMessage)
    }

    @Test
    fun invalidCharacterShowsRuleError() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("민수!")

        val state = viewModel.uiState.value
        assertFalse(state.canSubmit)
        assertEquals("한글·영문·숫자 2~10자로 입력해주세요", state.errorMessage)
    }

    @Test
    fun nicknameIsCappedAtMaxLength() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("가나다라마바사아자차카")

        assertEquals(10, viewModel.uiState.value.nickname.length)
    }

    @Test
    fun emptyNicknameClearsError() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("민")
        viewModel.onNicknameChange("")

        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun clearResetsNickname() {
        val viewModel = SignupViewModel(FakeSignupSessionRepository())

        viewModel.onNicknameChange("민수")
        viewModel.onClearClick()

        assertEquals("", viewModel.uiState.value.nickname)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitWithAvailableNicknameCompletesSignupAndNavigatesHome() = runTest {
        val repository = FakeSignupSessionRepository(
            checkResult = AppResult.Success(true),
            signupResult = AppResult.Success(authenticated()),
        )
        val viewModel = SignupViewModel(repository)
        viewModel.onNicknameChange("민수")

        viewModel.events.test {
            viewModel.onSubmit()

            assertEquals(SignupEvent.NavigateHome, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("민수"), repository.checkCalls)
        assertEquals(listOf("민수"), repository.signupCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun submitWithDuplicateNicknameShowsDuplicateErrorAndDoesNotSignup() = runTest {
        val repository = FakeSignupSessionRepository(
            checkResult = AppResult.Success(false),
        )
        val viewModel = SignupViewModel(repository)
        viewModel.onNicknameChange("민수")

        viewModel.onSubmit()

        assertEquals("이미 사용 중인 닉네임이에요", viewModel.uiState.value.errorMessage)
        assertTrue(repository.signupCalls.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun signupFailureEmitsSnackbar() = runTest {
        val repository = FakeSignupSessionRepository(
            checkResult = AppResult.Success(true),
            signupResult = AppResult.Failure(DomainError.Network),
        )
        val viewModel = SignupViewModel(repository)
        viewModel.onNicknameChange("민수")

        viewModel.events.test {
            viewModel.onSubmit()

            val event = awaitItem()
            assertTrue(event is SignupEvent.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun submitIsIgnoredWhenNicknameInvalid() = runTest {
        val repository = FakeSignupSessionRepository()
        val viewModel = SignupViewModel(repository)
        viewModel.onNicknameChange("민")

        viewModel.onSubmit()

        assertTrue(repository.checkCalls.isEmpty())
        assertTrue(repository.signupCalls.isEmpty())
    }

    private fun authenticated(): AuthState =
        AuthState.Authenticated(
            user = UserProfile(id = "1", displayName = "민수", phoneLast4 = null),
            isNewUser = false,
        )
}

private class FakeSignupSessionRepository(
    private val checkResult: AppResult<Boolean> = AppResult.Success(true),
    private val signupResult: AppResult<AuthState> =
        AppResult.Success(
            AuthState.Authenticated(
                user = UserProfile(id = "1", displayName = "민수", phoneLast4 = null),
                isNewUser = false,
            ),
        ),
) : SessionRepository {
    val checkCalls = mutableListOf<String>()
    val signupCalls = mutableListOf<String>()
    private val authState = MutableStateFlow<AuthState>(AuthState.Guest)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> = AppResult.Success(authState.value)

    override suspend fun checkNickname(nickname: String): AppResult<Boolean> {
        checkCalls += nickname
        return checkResult
    }

    override suspend fun completeSignup(nickname: String): AppResult<AuthState> {
        signupCalls += nickname
        if (signupResult is AppResult.Success) {
            authState.value = signupResult.data
        }
        return signupResult
    }

    override suspend fun clearSession(): AppResult<Unit> {
        authState.value = AuthState.Guest
        return AppResult.Success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SignupMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
