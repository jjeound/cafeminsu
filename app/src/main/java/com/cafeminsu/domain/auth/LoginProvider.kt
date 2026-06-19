package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState

interface LoginProvider {
    suspend fun login(): AppResult<AuthState>
    suspend fun logout(): AppResult<Unit>
}
