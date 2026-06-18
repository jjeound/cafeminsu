package com.cafeminsu.ui.feature.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class OrderStatusScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsOrderStatusContent() {
        composeRule.setContent {
            CafeTheme {
                OrderStatusScreen(
                    state = OrderStatusUiState.Content(
                        orderId = "order-42",
                        orderNumber = "M042",
                        status = OrderStatus.Paid,
                        headerTitle = "주문이 들어갔어요",
                        statusMessage = "주문번호를 확인하고 잠시만 기다려 주세요.",
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
                        steps = orderStatusSteps(OrderStatus.Paid),
                    ),
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("주문이 들어갔어요").assertIsDisplayed()
        composeRule.onNodeWithText("주문번호 M042").assertIsDisplayed()
        composeRule.onNodeWithText("결제 완료").assertIsDisplayed()
        composeRule.onNodeWithText("민수 라떼").assertIsDisplayed()
        composeRule.onNodeWithText("2잔").assertIsDisplayed()
    }
}
