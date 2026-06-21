package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.toAppNotifications
import com.cafeminsu.data.remote.BaseResponse
import com.cafeminsu.data.remote.NotificationApi
import com.cafeminsu.data.remote.NotificationReadAllRes
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toDomainError
import com.cafeminsu.data.remote.unwrap
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealNotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val sessionStateHolder: SessionStateHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {
    override fun observeNotifications(): Flow<AppResult<List<AppNotification>>> =
        flow {
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(auth)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        notificationApi.getNotifications()
                    }
                ) {
                    is AppResult.Success -> response.data.unwrap { it.toAppNotifications() }
                    is AppResult.Failure -> response
                },
            )
        }.flowOn(ioDispatcher)

    override suspend fun markAllRead(): AppResult<Unit> =
        withContext(ioDispatcher) {
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            when (
                val response = runCatchingToAppResult {
                    notificationApi.markAllRead()
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

    private fun BaseResponse<NotificationReadAllRes>.toUnitResult(): AppResult<Unit> =
        if (isSuccess == true) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(code.toDomainErrorOrUnknown())
        }

    private fun Int?.toDomainErrorOrUnknown(): DomainError =
        this?.toDomainError() ?: DomainError.Unknown
}
