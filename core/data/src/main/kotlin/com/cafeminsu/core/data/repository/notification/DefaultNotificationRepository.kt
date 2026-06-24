package com.cafeminsu.core.data.repository.notification

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.model.notification.AppNotification
import com.cafeminsu.core.network.client.NotificationClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultNotificationRepository @Inject constructor(
    private val client: NotificationClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {
    override fun getNotifications(): Flow<List<AppNotification>> = flow {
        emit(client.getNotifications().map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun markAllRead(): Flow<Unit> = flow {
        emit(client.markAllRead())
    }.flowOn(ioDispatcher)
}
