package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.toOrder
import com.cafeminsu.data.mapper.toOrderCreateReq
import com.cafeminsu.data.mapper.toOrders
import com.cafeminsu.data.remote.DefaultOrderPage
import com.cafeminsu.data.remote.DefaultOrderPageSize
import com.cafeminsu.data.remote.OrderApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.unwrap
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OrderRepository {
    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        withContext(ioDispatcher) {
            if (cart.items.isEmpty() || cart.validation != CartValidation.Valid) {
                return@withContext AppResult.Failure(DomainError.Validation("cart"))
            }

            val userId = when (val result = currentUserId()) {
                is AppResult.Success -> result.data
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
                    orderApi.createOrder(
                        userId = userId,
                        request = request,
                    )
                }
            ) {
                is AppResult.Success -> response.data.unwrap {
                    it.toOrder(
                        cartItems = cart.items,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                }

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

            val userId = when (val result = currentUserId()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    emit(result)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        orderApi.getOrder(
                            orderId = serverOrderId,
                            userId = userId,
                        )
                    }
                ) {
                    is AppResult.Success -> response.data.unwrap { it.toOrder() }
                    is AppResult.Failure -> response
                },
            )
        }.flowOn(ioDispatcher)

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        flow {
            val userId = when (val result = currentUserId()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    emit(result)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        orderApi.getMyOrders(
                            userId = userId,
                            page = DefaultOrderPage,
                            size = DefaultOrderPageSize,
                        )
                    }
                ) {
                    is AppResult.Success -> response.data.unwrap { it.toOrders() }
                    is AppResult.Failure -> response
                },
            )
        }.flowOn(ioDispatcher)

    private fun currentUserId(): AppResult<Long> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }

        val userId = authState.user.id.toLongOrNull()
            ?: return AppResult.Failure(DomainError.Validation("userId"))
        return AppResult.Success(userId)
    }

    private fun currentStoreId(): AppResult<Long> {
        val storeId = selectedStoreHolder.current()?.id?.toLongOrNull()
            ?: return AppResult.Failure(DomainError.NotFound)
        return AppResult.Success(storeId)
    }
}
