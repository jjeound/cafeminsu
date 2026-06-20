package com.cafeminsu.ui.feature.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Rule
import org.junit.Test

class OrderResultScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successScreenShowsOrderSummaryAndActions() {
        composeRule.setContent {
            CafeTheme {
                OrderResultScreen(
                    state = OrderResultUiState.Content(sampleSummary()),
                    onCloseClick = {},
                    onStatusClick = {},
                    onHomeClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("주문이 완료됐어요").assertIsDisplayed()
        composeRule.onNodeWithText("A-2543").assertIsDisplayed()
        composeRule.onNodeWithText("카페민수 강남점").assertIsDisplayed()
        composeRule.onNodeWithText("스탬프 1개가 적립됐어요 (8/10)").assertIsDisplayed()
        composeRule.onNodeWithText("주문 상태 보기").assertIsDisplayed()
        composeRule.onNodeWithText("홈으로 이동").assertIsDisplayed()
    }

    @Test
    fun failureDialogShowsMessageErrorCodeAndActions() {
        composeRule.setContent {
            CafeTheme {
                OrderFailureDialog(
                    failure = OrderFailureUiModel(
                        title = "결제에 실패했어요",
                        message = "카드 한도 초과 또는 정보 오류로\n결제가 처리되지 않았어요.",
                        errorCode = "ERR_PAY_LIMIT_EX",
                    ),
                    onCancel = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("결제에 실패했어요").assertIsDisplayed()
        composeRule.onNodeWithText("ERR_PAY_LIMIT_EX").assertIsDisplayed()
        composeRule.onNodeWithText("취소").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
    }
}

private fun sampleSummary(): OrderSuccessSummary =
    OrderSuccessSummary(
        orderId = "order-1",
        orderNumber = "A-2543",
        pickupStoreName = "카페민수 강남점",
        estimatedReadyLabel = "약 8분 후",
        paidAmountLabel = "8,500원",
        stampMessage = "스탬프 1개가 적립됐어요 (8/10)",
    )
