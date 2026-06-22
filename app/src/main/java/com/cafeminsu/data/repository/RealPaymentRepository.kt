package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.PreparedPayment
import com.cafeminsu.data.mapper.toPaymentResult
import com.cafeminsu.data.mapper.toPreparedPayment
import com.cafeminsu.data.payment.PgClient
import com.cafeminsu.data.remote.PaymentApi
import com.cafeminsu.data.remote.PaymentPrepareReq
import com.cafeminsu.data.remote.PaymentVerifyReq
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.repository.PaymentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class RealPaymentRepository @Inject constructor(
    private val paymentApi: PaymentApi,
    private val pgClient: PgClient,
    private val sessionStateHolder: SessionStateHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PaymentRepository {
    private val preparedPaymentsByKey = mutableMapOf<String, PreparedPayment>()
    private val paymentIdsByKey = mutableMapOf<String, Long>()

    override suspend fun pay(request: PaymentRequest): AppResult<PaymentResult> =
        withContext(ioDispatcher) {
            validate(request)?.let { error -> return@withContext AppResult.Failure(error) }

            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext result
            }
            val orderId = request.orderId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.Validation("orderId"))
            val preparedPayment = when (val result = preparePayment(orderId, request)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }
            val impUid = when (
                val result = pgClient.authorize(
                    merchantUid = preparedPayment.merchantUid,
                    amount = preparedPayment.amount,
                )
            ) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }

            when (
                val response = runCatchingToAppResult {
                    paymentApi.verify(
                        request = PaymentVerifyReq(
                            impUid = impUid,
                            merchantUid = preparedPayment.merchantUid,
                        ),
                    )
                }
            ) {
                is AppResult.Success -> {
                    val mapped = response.data.toPaymentResult(orderId = request.orderId)
                    if (mapped is AppResult.Success) {
                        mapped.data.paymentId.toLongOrNull()?.let { paymentId ->
                            paymentIdsByKey[request.idempotencyKey] = paymentId
                        }
                    }
                    mapped
                }

                is AppResult.Failure -> response
            }
        }

    override suspend fun getPaymentStatus(
        orderId: String,
        idempotencyKey: String,
    ): AppResult<PaymentResult> =
        withContext(ioDispatcher) {
            if (orderId.isBlank()) {
                return@withContext AppResult.Failure(DomainError.Validation("orderId"))
            }
            if (idempotencyKey.isBlank()) {
                return@withContext AppResult.Failure(DomainError.Validation("idempotencyKey"))
            }

            when (val result = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext result
            }
            val paymentId = paymentIdsByKey[idempotencyKey]
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (
                val response = runCatchingToAppResult {
                    paymentApi.getPayment(paymentId = paymentId)
                }
            ) {
                is AppResult.Success -> {
                    when (val mapped = response.data.toPaymentResult()) {
                        is AppResult.Success -> {
                            if (mapped.data.orderId == orderId) {
                                mapped
                            } else {
                                AppResult.Failure(DomainError.Payment("order-mismatch"))
                            }
                        }

                        is AppResult.Failure -> mapped
                    }
                }

                is AppResult.Failure -> response
            }
        }

    private suspend fun preparePayment(
        orderId: Long,
        request: PaymentRequest,
    ): AppResult<PreparedPayment> {
        preparedPaymentsByKey[request.idempotencyKey]?.let { prepared ->
            return if (prepared.orderId == request.orderId) {
                AppResult.Success(prepared)
            } else {
                AppResult.Failure(DomainError.Payment("idempotency-key-conflict"))
            }
        }

        return when (
            val response = runCatchingToAppResult {
                paymentApi.prepare(
                    request = PaymentPrepareReq(
                        orderId = orderId,
                        cardAmount = request.amount,
                    ),
                )
            }
        ) {
            is AppResult.Success -> {
                val mapped = response.data.toPreparedPayment(orderId = request.orderId)
                if (mapped is AppResult.Success) {
                    preparedPaymentsByKey[request.idempotencyKey] = mapped.data
                }
                mapped
            }

            is AppResult.Failure -> response
        }
    }

    private fun validate(request: PaymentRequest): DomainError? =
        when {
            request.orderId.isBlank() -> DomainError.Validation("orderId")
            request.amount <= 0 -> DomainError.Validation("amount")
            request.paymentMethodToken.isBlank() -> DomainError.Payment("invalid-payment-token")
            request.idempotencyKey.isBlank() -> DomainError.Validation("idempotencyKey")
            else -> null
        }

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }
}
