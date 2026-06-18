package com.cafeminsu.ui.feature.payment

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.PaymentRepository
import com.cafeminsu.ui.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    @get:Rule
    val mainDispatcherRule = PaymentMainDispatcherRule()

    @Test
    fun approvedPaymentEmitsSuccessEventForOrderId() = runTest {
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Approved)),
            ),
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.onPay()

                assertEquals(PaymentEvent.PaymentApproved("order-1"), awaitItem())
                assertTrue(awaitContentWithProgress<PaymentProgress.Approved>().paymentState is PaymentProgress.Approved)
                assertEquals(1, paymentRepository.payRequests.size)
                assertEquals("order-1", paymentRepository.payRequests.single().orderId)
                assertEquals(12_000, paymentRepository.payRequests.single().amount)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun approvedPaymentForDifferentOrderDoesNotEmitSuccessEvent() = runTest {
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(
                    paymentResult(
                        orderId = "other-order",
                        status = PaymentStatus.Approved,
                    ),
                ),
            ),
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.onPay()

                awaitContentWithProgress<PaymentProgress.NeedsConfirmation>()
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failedPaymentDoesNotEmitSuccessEvent() = runTest {
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Failed)),
            ),
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.onPay()

                val failed = awaitContentWithProgress<PaymentProgress.Failed>()
                val progress = failed.paymentState as PaymentProgress.Failed
                assertTrue(progress.message.isNotBlank())
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownPaymentConfirmsStatusAndDoesNotOptimisticallySucceedWhenUnresolved() = runTest {
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Unknown)),
            ),
            statusResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Unknown)),
            ),
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.events.test {
                viewModel.onPay()

                val needsConfirmation = awaitContentWithProgress<PaymentProgress.NeedsConfirmation>()
                val progress = needsConfirmation.paymentState as PaymentProgress.NeedsConfirmation
                assertTrue(progress.message.isNotBlank())
                assertEquals(1, paymentRepository.statusRequests.size)
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retryForSameOrderReusesIdempotencyKey() = runTest {
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Failed)),
                AppResult.Success(paymentResult(status = PaymentStatus.Failed)),
            ),
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.onPay()
            awaitContentWithProgress<PaymentProgress.Failed>()

            viewModel.onPay()
            awaitContentWithProgress<PaymentProgress.Failed>()

            assertEquals(2, paymentRepository.payRequests.size)
            assertEquals(
                paymentRepository.payRequests.first().idempotencyKey,
                paymentRepository.payRequests.last().idempotencyKey,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun payWhileProcessingIgnoresDuplicateTap() = runTest {
        val payGate = CompletableDeferred<Unit>()
        val paymentRepository = FakePaymentRepository(
            payResults = mutableListOf(
                AppResult.Success(paymentResult(status = PaymentStatus.Approved)),
            ),
            payGate = payGate,
        )
        val viewModel = viewModel(paymentRepository = paymentRepository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.onPay()
            assertTrue(awaitContentWithProgress<PaymentProgress.Processing>().isPayEnabled.not())

            viewModel.onPay()
            assertEquals(1, paymentRepository.payRequests.size)

            viewModel.events.test {
                payGate.complete(Unit)

                assertEquals(PaymentEvent.PaymentApproved("order-1"), awaitItem())
                assertFalse(awaitContent().paymentState is PaymentProgress.Processing)
                assertEquals(1, paymentRepository.payRequests.size)

                cancelAndIgnoreRemainingEvents()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        orderId: String = "order-1",
        orderRepository: FakePaymentOrderRepository = FakePaymentOrderRepository(
            initialResult = AppResult.Success(sampleOrder(id = orderId)),
        ),
        paymentRepository: FakePaymentRepository = FakePaymentRepository(),
    ): PaymentViewModel =
        PaymentViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.PAYMENT_ORDER_ID to orderId)),
            paymentRepository = paymentRepository,
            orderRepository = orderRepository,
        )

    private suspend fun ReceiveTurbine<PaymentUiState>.awaitContent(): PaymentUiState.Content {
        while (true) {
            val state = awaitItem()
            if (state is PaymentUiState.Content) {
                return state
            }
        }
    }

    private suspend inline fun <reified T : PaymentProgress> ReceiveTurbine<PaymentUiState>.awaitContentWithProgress():
        PaymentUiState.Content {
        while (true) {
            val content = awaitContent()
            if (content.paymentState is T) {
                return content
            }
        }
    }
}

private class FakePaymentOrderRepository(
    initialResult: AppResult<Order>,
) : OrderRepository {
    private val order = MutableStateFlow(initialResult)
    val observedOrderIds = mutableListOf<String>()

    override suspend fun createOrderFromCart(cart: Cart): AppResult<Order> =
        AppResult.Failure(DomainError.Unknown)

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> {
        observedOrderIds += orderId
        return order
    }

    override fun observeOrderHistory(): Flow<AppResult<List<Order>>> =
        MutableStateFlow(AppResult.Success(emptyList()))
}

private class FakePaymentRepository(
    private val payResults: MutableList<AppResult<PaymentResult>> = mutableListOf(
        AppResult.Success(paymentResult(status = PaymentStatus.Approved)),
    ),
    private val statusResults: MutableList<AppResult<PaymentResult>> = mutableListOf(
        AppResult.Failure(DomainError.NotFound),
    ),
    private val payGate: CompletableDeferred<Unit>? = null,
) : PaymentRepository {
    val payRequests = mutableListOf<PaymentRequest>()
    val statusRequests = mutableListOf<StatusRequest>()

    override suspend fun pay(request: PaymentRequest): AppResult<PaymentResult> {
        payRequests += request
        payGate?.await()
        return payResults.removeFirstOrNull() ?: AppResult.Failure(DomainError.Unknown)
    }

    override suspend fun getPaymentStatus(
        orderId: String,
        idempotencyKey: String,
    ): AppResult<PaymentResult> {
        statusRequests += StatusRequest(orderId, idempotencyKey)
        return statusResults.removeFirstOrNull() ?: AppResult.Failure(DomainError.NotFound)
    }
}

private data class StatusRequest(
    val orderId: String,
    val idempotencyKey: String,
)

private fun paymentResult(
    orderId: String = "order-1",
    status: PaymentStatus,
): PaymentResult =
    PaymentResult(
        orderId = orderId,
        paymentId = "payment-1",
        status = status,
        approvedAtMillis = if (status == PaymentStatus.Approved) 1_800_000_000_000L else null,
    )

private fun sampleOrder(
    id: String = "order-1",
): Order =
    Order(
        id = id,
        orderNumber = "M001",
        items = listOf(
            CartItem(
                id = "cart-item-1",
                menuItemId = "latte",
                name = "민수 라떼",
                unitPrice = 6_000,
                selectedOptions = emptyList(),
                quantity = 2,
            ),
        ),
        totalAmount = 12_000,
        status = OrderStatus.PendingPayment,
        createdAtMillis = 1_800_000_000_000L,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
