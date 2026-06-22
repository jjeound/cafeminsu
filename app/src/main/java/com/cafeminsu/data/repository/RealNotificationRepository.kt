package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.local.notification.NotificationLocalDataSource
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
    private val localDataSource: NotificationLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {
    override fun observeNotifications(): Flow<AppResult<List<AppNotification>>> =
        flow {
            // 인증 게이트를 먼저 통과해야만 캐시도 조회한다 — 미인증 시 다른 사용자 캐시 노출 금지(보안).
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
                    is AppResult.Success ->
                        when (val mapped = response.data.unwrap { it.toAppNotifications() }) {
                            is AppResult.Success -> {
                                // 성공 시 write-through 후 그대로 방출.
                                localDataSource.replaceNotifications(mapped.data)
                                mapped
                            }
                            is AppResult.Failure -> mapped
                        }
                    is AppResult.Failure -> {
                        // 오프라인 폴백: 캐시가 있으면 읽기 전용으로 노출, 없으면 실패 전파.
                        val cached = localDataSource.cachedNotifications()
                        if (cached.isEmpty()) response else AppResult.Success(cached)
                    }
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
