package com.cafeminsu.ui.feature.store

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafeminsu.ui.theme.CafeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StoreScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsStoreContentAndBottomSheetDetail() {
        var detailRequestedStoreId: String? = null
        var startRequestedStoreId: String? = null

        composeRule.setContent {
            CafeTheme {
                StoreScreen(
                    state = sampleContentState(selectedStore = sampleDetail()),
                    onQueryChange = {},
                    onStoreClick = { detailRequestedStoreId = it },
                    onDismissStoreDetail = {},
                    onStartOrder = { startRequestedStoreId = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("매장 선택").assertIsDisplayed()
        composeRule.onNodeWithText("오늘 어디서 한 잔 하실까요?").assertIsDisplayed()
        composeRule.onNodeWithText("현재 위치 또는 매장명 검색").assertIsDisplayed()
        composeRule.onNodeWithText("내 주변 지도").assertIsDisplayed()
        composeRule.onNodeWithText("가까운 매장").assertIsDisplayed()
        composeRule.onNodeWithText("카페민수 역삼점").assertIsDisplayed()
        composeRule.onNodeWithText("이 매장에서 주문하기").assertIsDisplayed()

        composeRule.onNodeWithText("카페민수 역삼점").performClick()
        composeRule.onNodeWithText("이 매장에서 주문하기").performClick()

        composeRule.runOnIdle {
            assertEquals("yeoksam", detailRequestedStoreId)
            assertEquals("gangnam", startRequestedStoreId)
        }
    }

    private fun sampleContentState(selectedStore: StoreDetailUiModel?): StoreUiState.Content =
        StoreUiState.Content(
            query = "",
            stores = listOf(
                StoreUiModel(
                    id = "gangnam",
                    name = "카페민수 강남점",
                    address = "서울 강남구 테헤란로 134",
                    distanceLabel = "120m",
                    status = StoreStatusUiModel.Open,
                    statusLabel = "영업중",
                ),
                StoreUiModel(
                    id = "yeoksam",
                    name = "카페민수 역삼점",
                    address = "서울 강남구 역삼로 92",
                    distanceLabel = "340m",
                    status = StoreStatusUiModel.Open,
                    statusLabel = "영업중",
                ),
            ),
            selectedStore = selectedStore,
        )

    private fun sampleDetail(): StoreDetailUiModel =
        StoreDetailUiModel(
            id = "gangnam",
            name = "카페민수 강남점",
            statusLabel = "영업중 · 22:00 마감",
            address = "서울 강남구 테헤란로 134",
            phone = "02-3456-7890",
            distanceLabel = "현재 위치에서 120m",
            parkingLabel = "건물 내 30분 무료",
            amenities = listOf("콘센트", "Wi-Fi", "드라이브스루", "테라스"),
        )
}
