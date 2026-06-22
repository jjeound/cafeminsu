package com.cafeminsu.ui.feature.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.OrderType
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CartScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEmptyCartAction() {
        composeRule.setContent {
            CafeTheme {
                CartScreen(
                    state = CartUiState.Empty(
                        message = "담은 메뉴가 없어요",
                        validation = CartValidation.Invalid(emptyList()),
                        checkoutInProgress = false,
                        orderType = OrderType.DineIn,
                        requestNote = "",
                    ),
                    onBackClick = {},
                    onQuantityChange = { _, _ -> },
                    onOrderTypeSelected = {},
                    onRequestNoteChange = {},
                    onCheckout = {},
                    onRetry = {},
                    onBrowseMenuClick = {},
                    onItemClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("장바구니").assertIsDisplayed()
        composeRule.onNodeWithText("담은 메뉴가 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("메뉴 보러가기").assertIsDisplayed()
    }

    @Test
    fun showsCartItemAndCheckoutButton() {
        composeRule.setContent {
            CafeTheme {
                CartScreen(
                    state = CartUiState.Content(
                        items = listOf(sampleCartItem()),
                        subtotal = 11_400,
                        validation = CartValidation.Valid,
                        checkoutInProgress = false,
                        orderType = OrderType.DineIn,
                        requestNote = "",
                    ),
                    onBackClick = {},
                    onQuantityChange = { _, _ -> },
                    onOrderTypeSelected = {},
                    onRequestNoteChange = {},
                    onCheckout = {},
                    onRetry = {},
                    onBrowseMenuClick = {},
                    onItemClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("민수 라떼").assertIsDisplayed()
        composeRule.onNodeWithText("라지").assertIsDisplayed()
        composeRule.onNodeWithText("주문 방식").assertIsDisplayed()
        composeRule.onNodeWithText("요청사항").assertIsDisplayed()
        composeRule.onNodeWithText("결제하기").assertIsDisplayed()
        composeRule.onNodeWithText("총 결제 금액").assertIsDisplayed()
        composeRule.onNodeWithText("11,400원").assertIsDisplayed()
    }

    @Test
    fun tappingItemNavigatesToMenuDetailWithCartItemId() {
        var clicked: Pair<String, String>? = null

        composeRule.setContent {
            CafeTheme {
                CartScreen(
                    state = CartUiState.Content(
                        items = listOf(sampleCartItem()),
                        subtotal = 11_400,
                        validation = CartValidation.Valid,
                        checkoutInProgress = false,
                        orderType = OrderType.DineIn,
                        requestNote = "",
                    ),
                    onBackClick = {},
                    onQuantityChange = { _, _ -> },
                    onOrderTypeSelected = {},
                    onRequestNoteChange = {},
                    onCheckout = {},
                    onRetry = {},
                    onBrowseMenuClick = {},
                    onItemClick = { menuItemId, cartItemId -> clicked = menuItemId to cartItemId },
                )
            }
        }

        composeRule.onNodeWithText("민수 라떼").performClick()

        composeRule.runOnIdle {
            assertEquals("latte" to "cart-item-1", clicked)
        }
    }

    @Test
    fun decreasingQuantityToZeroRequestsRemoval() {
        var lastQuantityChange: Pair<String, Int>? = null

        composeRule.setContent {
            CafeTheme {
                CartScreen(
                    state = CartUiState.Content(
                        items = listOf(sampleCartItem(quantity = 1)),
                        subtotal = 5_700,
                        validation = CartValidation.Valid,
                        checkoutInProgress = false,
                        orderType = OrderType.DineIn,
                        requestNote = "",
                    ),
                    onBackClick = {},
                    onQuantityChange = { id, quantity -> lastQuantityChange = id to quantity },
                    onOrderTypeSelected = {},
                    onRequestNoteChange = {},
                    onCheckout = {},
                    onRetry = {},
                    onBrowseMenuClick = {},
                    onItemClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("−").performClick()

        composeRule.runOnIdle {
            assertEquals("cart-item-1" to 0, lastQuantityChange)
        }
    }

    private fun sampleCartItem(quantity: Int = 2): CartItem =
        CartItem(
            id = "cart-item-1",
            menuItemId = "latte",
            name = "민수 라떼",
            unitPrice = 5_700,
            selectedOptions = listOf(
                SelectedOption(
                    groupId = "size",
                    optionId = "large",
                    name = "라지",
                    extraPrice = 700,
                ),
            ),
            quantity = quantity,
        )
}
