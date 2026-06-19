package com.cafeminsu.ui.feature.payment

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class PaymentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOrderAmountAndPaymentMethodsWithoutTokenText() {
        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(),
                    onSelectMethod = {},
                    onPay = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제").assertIsDisplayed()
        composeRule.onNodeWithText("M001").assertIsDisplayed()
        composeRule.onNodeWithText("12,000원 결제").assertIsDisplayed()
        composeRule.onNodeWithText("민수페이").assertIsDisplayed()
        composeRule.onNodeWithText("카드(등록됨)").assertIsDisplayed()
        composeRule.onAllNodesWithText("tok_minsupay").assertCountEquals(0)
        composeRule.onAllNodesWithText("tok_card_demo").assertCountEquals(0)
    }

    @Test
    fun showsProcessingCopyAndDisablesOptimisticSuccess() {
        composeRule.setContent {
            CafeTheme {
                PaymentScreen(
                    state = contentState(paymentState = PaymentProgress.Processing),
                    onSelectMethod = {},
                    onPay = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onAllNodesWithText("결제 처리 중").assertCountEquals(2)
        composeRule.onNodeWithText("승인 결과를 확인하고 있어요.").assertIsDisplayed()
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
                            groupId = "size",
                            optionId = "size-large",
                            name = "라지",
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
