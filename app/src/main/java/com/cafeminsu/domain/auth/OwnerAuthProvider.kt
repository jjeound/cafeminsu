package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.OwnerProfile

interface OwnerAuthProvider {
    suspend fun login(loginId: String, password: String): AppResult<OwnerProfile>
    suspend fun logout(): AppResult<Unit>
    suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile>
}
