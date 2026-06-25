package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.local.order.OrderHistoryLocalDataSource
import com.cafeminsu.data.mapper.toOrder
import com.cafeminsu.data.mapper.toOrderCreateReq
import com.cafeminsu.data.mapper.toOrders
import com.cafeminsu.data.remote.DefaultOrderPage
import com.cafeminsu.data.remote.DefaultOrderPageSize
import com.cafeminsu.data.remote.OrderApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.repository.OrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealOrderRepository @Inject constructor(
    private val orderApi: OrderApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    private val sessionStateHolder: SessionStateHolder,
    private val localDataSource: OrderHistoryLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OrderRepository {
    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        withContext(ioDispatcher) {
            if (cart.items.isEmpty() || cart.validation != CartValidation.Valid) {
                return@withContext AppResult.Failure(DomainError.Validation("cart"))
            }

            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext result
            }
            val storeId = when (val result = currentStoreId()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }
            val request = when (val result = cart.toOrderCreateReq(storeId)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }

            when (
                val response = runCatchingToAppResult {
                    orderApi.createOrder(request = request)
                }
            ) {
                is AppResult.Success -> response.data.toOrder(
                    cartItems = cart.items,
                    createdAtMillis = System.currentTimeMillis(),
                )

                is AppResult.Failure -> response
            }
        }

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        flow {
            val serverOrderId = orderId.toLongOrNull()
            if (serverOrderId == null) {
                emit(AppResult.Failure(DomainError.NotFound))
                return@flow
            }

            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(result)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        orderApi.getOrder(orderId = serverOrderId)
                    }
                ) {
                    is AppResult.Success -> response.data.toOrder()
                    is AppResult.Failure -> response
                },
            )
        }.flowOn(ioDispatcher)

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        flow {
            // 인증 게이트를 먼저 통과해야만 캐시도 조회한다 — 미인증 시 다른 사용자 캐시 노출 금지(보안).
            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(result)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        orderApi.getMyOrders(
                            page = DefaultOrderPage,
                            size = DefaultOrderPageSize,
                        )
                    }
                ) {
                    is AppResult.Success ->
                        // 목록 응답이 이제 라인아이템(items)을 포함하므로 상세 보강 없이 그대로 매핑·write-through.
                        when (val mapped = response.data.toOrders()) {
                            is AppResult.Success -> {
                                localDataSource.replaceHistory(mapped.data)
                                mapped
                            }
                            is AppResult.Failure -> mapped
                        }
                    is AppResult.Failure -> {
                        // 오프라인 폴백: 캐시가 있으면 읽기 전용으로 노출, 없으면 실패 전파.
                        val cached = localDataSource.cachedHistory()
                        if (cached.isEmpty()) response else AppResult.Success(cached)
                    }
                },
            )
        }.flowOn(ioDispatcher)

    override fun observeRecentOrders(): Flow<AppResult<List<Order>>> =
        flow {
            // 전체 내역과 동일하게 인증 게이트를 먼저 통과한다(미인증 시 캐시도 보지 않음).
            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(result)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        orderApi.getRecentOrders()
                    }
                ) {
                    // 최근 주문도 items 를 포함한다. 전체 내역 캐시를 덮어쓰지 않도록 write-through 는 하지 않고,
                    // 실패 시에는 읽기 전용 캐시 폴백만 한다.
                    is AppResult.Success -> response.data.toOrders()
                    is AppResult.Failure -> {
                        val cached = localDataSource.cachedHistory()
                        if (cached.isEmpty()) response else AppResult.Success(cached)
                    }
                },
            )
        }.flowOn(ioDispatcher)

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }

    private fun currentStoreId(): AppResult<Long> {
        val storeId = selectedStoreHolder.current()?.id?.toLongOrNull()
            ?: return AppResult.Failure(DomainError.NotFound)
        return AppResult.Success(storeId)
    }
}
