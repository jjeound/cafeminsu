package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.BaseResponse
import com.cafeminsu.data.remote.FcmTokenApi
import com.cafeminsu.data.remote.FcmTokenReq
import com.cafeminsu.data.remote.FcmTokenRes
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toDomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.FcmTokenRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class RealFcmTokenRepository @Inject constructor(
    private val fcmTokenApi: FcmTokenApi,
    private val sessionStateHolder: SessionStateHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FcmTokenRepository {
    override suspend fun register(token: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            when (
                val response = runCatchingToAppResult {
                    fcmTokenApi.updateFcmToken(FcmTokenReq(fcmToken = token))
                }
            ) {
                is AppResult.Success -> response.data.toUnitResult()
                is AppResult.Failure -> response
            }
        }

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }

    private fun BaseResponse<FcmTokenRes>.toUnitResult(): AppResult<Unit> =
        if (isSuccess == true) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(code.toDomainErrorOrUnknown())
        }

    private fun Int?.toDomainErrorOrUnknown(): DomainError =
        this?.toDomainError() ?: DomainError.Unknown
}
