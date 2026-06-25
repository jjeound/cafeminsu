package com.cafeminsu.data.auth

import com.cafeminsu.domain.model.AuthState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SessionStateHolder @Inject constructor() {
    constructor(initialAuthState: AuthState) : this() {
        authState.value = initialAuthState
    }

    internal val authState = MutableStateFlow<AuthState>(AuthState.Unknown)

    fun observe(): StateFlow<AuthState> = authState.asStateFlow()

    fun update(authState: AuthState) {
        this.authState.value = authState
    }
}
