package com.cafeminsu.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.core.DataUiState
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StateComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dataUiStateContentRendersLoadingView() {
        composeRule.setContent {
            CafeTheme {
                DataUiStateContent<String>(
                    state = DataUiState.Loading,
                    onRetry = {},
                ) {
                    Text(text = it)
                }
            }
        }

        composeRule.onNodeWithText("불러오는 중").assertIsDisplayed()
    }

    @Test
    fun dataUiStateContentRendersEmptyViewWithAction() {
        var actionClicks = 0

        composeRule.setContent {
            CafeTheme {
                DataUiStateContent<String>(
                    state = DataUiState.Empty("담긴 메뉴가 없습니다"),
                    onRetry = {},
                    emptyActionLabel = "메뉴 보러가기",
                    onEmptyAction = { actionClicks += 1 },
                ) {
                    Text(text = it)
                }
            }
        }

        composeRule.onNodeWithText("담긴 메뉴가 없습니다").assertIsDisplayed()
        composeRule.onNodeWithText("메뉴 보러가기").performClick()

        composeRule.runOnIdle {
            assertEquals(1, actionClicks)
        }
    }

    @Test
    fun dataUiStateContentRendersRetryableErrorView() {
        var retries = 0

        composeRule.setContent {
            CafeTheme {
                DataUiStateContent<String>(
                    state = DataUiState.Error("메뉴를 불러오지 못했어요.", retryable = true),
                    onRetry = { retries += 1 },
                ) {
                    Text(text = it)
                }
            }
        }

        composeRule.onNodeWithText("메뉴를 불러오지 못했어요.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()

        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun dataUiStateContentRendersOfflineBannerAndCachedContent() {
        composeRule.setContent {
            CafeTheme {
                DataUiStateContent(
                    state = DataUiState.Offline("캐시 메뉴"),
                    onRetry = {},
                ) {
                    Text(text = it)
                }
            }
        }

        composeRule.onNodeWithText("오프라인 상태입니다").assertIsDisplayed()
        composeRule.onNodeWithText("저장된 내용을 읽기 전용으로 표시합니다.").assertIsDisplayed()
        composeRule.onNodeWithText("캐시 메뉴").assertIsDisplayed()
    }

    @Test
    fun cafeSnackbarHostRendersCafeSnackbarMessage() {
        composeRule.setContent {
            CafeTheme {
                val hostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    hostState.cafeSnackbar(
                        message = "저장됐습니다",
                        type = CafeSnackbarType.Success,
                    )
                }

                CafeSnackbarHost(hostState = hostState)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("저장됐습니다").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("저장됐습니다").assertIsDisplayed()
    }
}
