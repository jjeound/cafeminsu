package com.ssafy.cafeminsu.core.data.repository.auth

import com.ssafy.cafeminsu.core.model.auth.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    fun syncAuthState(): Flow<AuthState>

    fun signInWithKakao(): Flow<AuthState>

    fun signOut(): Flow<Unit>

    fun isNicknameAvailable(nickname: String): Flow<Boolean>

    fun completeSignUp(nickname: String): Flow<AuthState>

    fun clearAuthState(): Flow<Unit>
}
