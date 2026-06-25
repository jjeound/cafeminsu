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
import javax.inject.Inject

class DefaultAuthRepository @Inject constructor(
    private val kakaoAuthDataSource: KakaoAuthDataSource,
    private val authClient: AuthClient,
    private val sessionPreferences: SessionPreferencesDataSource,
) : AuthRepository {
    private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Unknown)

    override val authState: Flow<AuthState> = mutableAuthState.asStateFlow()

    override fun signInWithKakao(): Flow<AuthState> = flow {
        val kakaoAccessToken = kakaoAuthDataSource.getAccessToken()
        val response = authClient.kakaoLogin(KakaoLoginRequest(kakaoAccessToken))
        sessionPreferences.setTokens(response.accessToken, response.refreshToken)
        emitAndStore(AuthState.Authenticated(UserProfile("server-user", response.nickname, PhoneLast4.Unavailable), isNewUser = response.isNewUser))
    }

    override fun syncAuthState(): Flow<AuthState> = flow {
        val tokens = sessionPreferences.tokens.first()
        if (tokens.refreshToken.isBlank()) {
            emitAndStore(AuthState.Guest)
        } else {
            val token = authClient.refresh(tokens.refreshToken)
            sessionPreferences.setAccessToken(token.accessToken)
            val profile = authClient.getMyProfile()
            emitAndStore(AuthState.Authenticated(UserProfile(profile.id.toString(), profile.nickname, PhoneLast4.Unavailable), if (profile.role == "OWNER") UserRole.Owner else UserRole.Customer))
        }
    }

    override fun isNicknameAvailable(nickname: String): Flow<Boolean> = flow {
        emit(authClient.checkNickname(nickname.trim()).available)
    }

    override fun completeSignUp(nickname: String): Flow<AuthState> = flow {
        val response = authClient.signup(SignupRequest(nickname.trim()))
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
}
