package com.cafeminsu.ui.feature.payment

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PaymentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsPayRedesignContentWithoutTokenText() {
        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(),
                    onBackClick = {},
                    onSelectMethod = {},
                    onToggleCoupon = {},
                    onPaymentSuccess = {},
                    onPaymentFailure = {},
                    onRetryFailure = {},
                    onDismissFailure = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제").assertIsDisplayed()
        composeRule.onNodeWithText("결제 수단").assertIsDisplayed()
        // 결제는 카카오페이로 통합 — 사용자가 보는 결제수단이 실제 PG 와 일치한다.
        composeRule.onNodeWithText("카카오페이").assertIsDisplayed()
        composeRule.onNodeWithText("주문 요약").assertIsDisplayed()
        composeRule.onNodeWithText("민수 라떼 (ICE/Reg) ✕ 2").assertIsDisplayed()
        composeRule.onNodeWithText("총 결제 금액").assertIsDisplayed()
        composeRule.onAllNodesWithText("12,000원").assertCountEquals(2)
        composeRule.onNodeWithText("결제하기").assertIsDisplayed()
        composeRule.onAllNodesWithText("tok_kakaopay_mock").assertCountEquals(0)
    }

    @Test
    fun payButtonInvokesPaymentSuccess() {
        var successClicks = 0

        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(),
                    onBackClick = {},
                    onSelectMethod = {},
                    onToggleCoupon = {},
                    onPaymentSuccess = { successClicks += 1 },
                    onPaymentFailure = {},
                    onRetryFailure = {},
                    onDismissFailure = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제하기").performClick()

        assertEquals(1, successClicks)
    }

    @Test
    fun showsProcessingCopyAndDisablesMockBranches() {
        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(paymentState = PaymentProgress.Processing),
                    onBackClick = {},
                    onSelectMethod = {},
                    onToggleCoupon = {},
                    onPaymentSuccess = {},
                    onPaymentFailure = {},
                    onRetryFailure = {},
                    onDismissFailure = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제 처리 중").assertIsDisplayed()
        composeRule.onNodeWithText("승인 결과를 확인하고 있어요.").assertIsDisplayed()
        composeRule.onNodeWithText("결제하기").assertIsNotEnabled()
    }

    @Test
    fun failedStateShowsOrderFailDialogAndActions() {
        var retryClicks = 0
        var cancelClicks = 0

        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(
                        paymentState = PaymentProgress.Failed(
                            paymentFailureUiModel(PaymentFailureReason.LimitExceeded),
                        ),
                    ),
                    onBackClick = {},
                    onSelectMethod = {},
                    onToggleCoupon = {},
                    onPaymentSuccess = {},
                    onPaymentFailure = {},
                    onRetryFailure = { retryClicks += 1 },
                    onDismissFailure = { cancelClicks += 1 },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제에 실패했어요").assertIsDisplayed()
        composeRule.onNodeWithText("ERR_PAY_LIMIT_EX").assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()
        composeRule.onNodeWithText("다시 시도").performClick()

        assertEquals(1, cancelClicks)
        assertEquals(1, retryClicks)
    }

    private fun contentState(
        paymentState: PaymentProgress = PaymentProgress.Idle,
    ): PaymentUiState.Content =
        PaymentUiState.Content(
            orderId = "order-1",
            orderNumber = "M001",
            items = listOf(
                CartItem(
                    id = "cart-item-1",
                    menuItemId = "latte",
                    name = "민수 라떼",
                    unitPrice = 6_000,
                    selectedOptions = listOf(
                        SelectedOption(
                            groupId = "temperature",
                            optionId = "ice",
                            name = "ICE",
                            extraPrice = 0,
                        ),
                        SelectedOption(
                            groupId = "size",
                            optionId = "regular",
                            name = "Reg",
                            extraPrice = 0,
                        ),
                    ),
                    quantity = 2,
                ),
            ),
            totalAmount = 12_000,
            methods = defaultPaymentMethods(),
            selectedMethodId = defaultPaymentMethods().first().id,
            paymentState = paymentState,
        )
}
