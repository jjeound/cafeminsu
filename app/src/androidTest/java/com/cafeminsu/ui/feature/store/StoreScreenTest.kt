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
        var mapMarkers: List<StoreMapMarker> = emptyList()

        composeRule.setContent {
            CafeTheme {
                StoreScreen(
                    state = sampleContentState(selectedStore = sampleDetail()),
                    query = "",
                    onQueryChange = {},
                    onStoreClick = { detailRequestedStoreId = it },
                    onDismissStoreDetail = {},
                    onStartOrder = { startRequestedStoreId = it },
                    onRetry = {},
                    // 실 MapView(GL/SDK) 인스턴스화를 피하려고 지도 슬롯을 스텁으로 주입.
                    mapContent = { markers, _ -> mapMarkers = markers },
                )
            }
        }

        composeRule.onNodeWithText("매장 선택").assertIsDisplayed()
        composeRule.onNodeWithText("오늘 어디서 한 잔 하실까요?").assertIsDisplayed()
        composeRule.onNodeWithText("현재 위치 또는 매장명 검색").assertIsDisplayed()
        composeRule.onNodeWithText("가까운 매장").assertIsDisplayed()
        composeRule.onNodeWithText("카페민수 역삼점").assertIsDisplayed()
        composeRule.onNodeWithText("이 매장에서 주문하기").assertIsDisplayed()

        composeRule.onNodeWithText("카페민수 역삼점").performClick()
        composeRule.onNodeWithText("이 매장에서 주문하기").performClick()

        composeRule.runOnIdle {
            assertEquals("yeoksam", detailRequestedStoreId)
            assertEquals("gangnam", startRequestedStoreId)
            assertEquals(listOf("gangnam", "yeoksam"), mapMarkers.map { it.id })
        }
    }

    @Test
    fun mapMarkerClickRequestsStoreDetail() {
        var detailRequestedStoreId: String? = null
        var capturedOnMarkerClick: ((String) -> Unit)? = null

        composeRule.setContent {
            CafeTheme {
                StoreScreen(
                    state = sampleContentState(selectedStore = null),
                    query = "",
                    onQueryChange = {},
                    onStoreClick = { detailRequestedStoreId = it },
                    onDismissStoreDetail = {},
                    onStartOrder = {},
                    onRetry = {},
                    // 지도 마커 탭이 onStoreClick(=상세/바텀시트) 로 이어지는지 스텁으로 검증.
                    mapContent = { _, onMarkerClick -> capturedOnMarkerClick = onMarkerClick },
                )
            }
        }

        composeRule.runOnIdle { capturedOnMarkerClick?.invoke("yeoksam") }

        composeRule.runOnIdle {
            assertEquals("yeoksam", detailRequestedStoreId)
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
                    latitude = 37.498,
                    longitude = 127.028,
                ),
                StoreUiModel(
                    id = "yeoksam",
                    name = "카페민수 역삼점",
                    address = "서울 강남구 역삼로 92",
                    distanceLabel = "340m",
                    status = StoreStatusUiModel.Open,
                    statusLabel = "영업중",
                    latitude = 37.500,
                    longitude = 127.036,
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
