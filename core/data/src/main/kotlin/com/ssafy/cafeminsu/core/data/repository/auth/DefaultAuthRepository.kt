package com.ssafy.cafeminsu.core.data.repository.auth

import com.ssafy.cafeminsu.core.data.kakao.KakaoAuthDataSource
import com.ssafy.cafeminsu.core.datastore.SessionPreferencesDataSource
import com.ssafy.cafeminsu.core.model.auth.AuthState
import com.ssafy.cafeminsu.core.model.auth.PhoneLast4
import com.ssafy.cafeminsu.core.model.auth.UserProfile
import com.ssafy.cafeminsu.core.model.auth.UserRole
import com.ssafy.cafeminsu.core.network.client.AuthClient
import com.ssafy.cafeminsu.core.network.model.request.auth.KakaoLoginRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.SignupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import android.util.Log
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val kakaoAuthDataSource: KakaoAuthDataSource,
    private val authClientProvider: Provider<AuthClient>,
    private val sessionPreferences: SessionPreferencesDataSource,
) : AuthRepository {
    private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Unknown)

    override val authState: Flow<AuthState> = mutableAuthState.asStateFlow()

    override fun signInWithKakao(): Flow<AuthState> = flow {
        Log.d(TAG, "signInWithKakao: start")
        val authClient = authClientProvider.get()
        val kakaoAccessToken = kakaoAuthDataSource.getAccessToken()
        val response = authClient.kakaoLogin(KakaoLoginRequest(kakaoAccessToken))
        sessionPreferences.setTokens(response.accessToken, response.refreshToken)
        Log.d(TAG, "signInWithKakao: tokens saved, isNewUser=${response.isNewUser}")
        emitAndStore(
            AuthState.Authenticated(
                UserProfile("server-user", response.nickname, PhoneLast4.Unavailable),
                isNewUser = response.isNewUser,
            ),
        )
    }

    override fun syncAuthState(): Flow<AuthState> = flow {
        val tokens = sessionPreferences.tokens.first()
        Log.d(TAG, "syncAuthState: stored access=${tokens.accessToken.isNotBlank()} refresh=${tokens.refreshToken.isNotBlank()}")
        if (tokens.refreshToken.isBlank()) {
            Log.d(TAG, "syncAuthState: no refresh token -> Guest")
            emitAndStore(AuthState.Guest)
        } else {
            try {
                val authClient = authClientProvider.get()
                val token = authClient.refresh(tokens.refreshToken)
                sessionPreferences.setAccessToken(token.accessToken)
                val profile = authClient.getMyProfile()
                Log.d(TAG, "syncAuthState: refresh/profile succeeded role=${profile.role}")
                emitAndStore(
                    AuthState.Authenticated(
                        UserProfile(profile.id.toString(), profile.nickname, PhoneLast4.Unavailable),
                        if (profile.role == "OWNER") UserRole.Owner else UserRole.Customer,
                    ),
                )
            } catch (throwable: Throwable) {
                Log.w(TAG, "syncAuthState: restore failed -> Guest", throwable)
                sessionPreferences.clearTokens()
                emitAndStore(AuthState.Guest)
            }
        }
    }

    override fun isNicknameAvailable(nickname: String): Flow<Boolean> = flow {
        emit(authClientProvider.get().checkNickname(nickname.trim()).available)
    }

    override fun completeSignUp(nickname: String): Flow<AuthState> = flow {
        val response = authClientProvider.get().signup(SignupRequest(nickname.trim()))
        emitAndStore(AuthState.Authenticated(UserProfile(response.userId.toString(), response.nickname, PhoneLast4.Unavailable)))
    }

    override fun signOut(): Flow<Unit> = flow {
        sessionPreferences.clearTokens()
        kakaoAuthDataSource.logout()
        mutableAuthState.value = AuthState.Guest
        emit(Unit)
    }

    override fun clearAuthState(): Flow<Unit> = flow {
        sessionPreferences.clearTokens()
        mutableAuthState.value = AuthState.Guest
        emit(Unit)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AuthState>.emitAndStore(state: AuthState) {
        mutableAuthState.value = state
        emit(state)
    }

    private companion object {
        private const val TAG = "DefaultAuthRepository"
    }
}
