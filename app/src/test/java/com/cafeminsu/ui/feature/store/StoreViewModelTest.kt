package com.cafeminsu.ui.feature.store

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus
import com.cafeminsu.domain.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {
    @get:Rule
    val mainDispatcherRule = StoreMainDispatcherRule()

    @Test
    fun initialLoadShowsNearbyStores() = runTest {
        val viewModel = StoreViewModel(
            FakeStoreRepository(
                stores = AppResult.Success(
                    listOf(
                        sampleStore(id = "gangnam", distanceMeters = 120),
                        sampleStore(id = "yeoksam", name = "카페민수 역삼점", distanceMeters = 340),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitSettledState() as StoreUiState.Content

            assertEquals("", content.query)
            assertEquals(listOf("gangnam", "yeoksam"), content.stores.map { it.id })
            assertEquals("120m", content.stores.first().distanceLabel)
            assertEquals("영업중", content.stores.first().statusLabel)
            assertEquals(37.498, content.stores.first().latitude, 0.0)
            assertEquals(127.028, content.stores.first().longitude, 0.0)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchQueryMapsEmptyResultsToEmptyState() = runTest {
        val viewModel = StoreViewModel(
            FakeStoreRepository(
                stores = AppResult.Success(listOf(sampleStore(id = "gangnam"))),
            ),
        )

        viewModel.uiState.test {
            assertTrue(awaitSettledState() is StoreUiState.Content)

            viewModel.onQueryChange("없는 매장")

            val empty = awaitItem() as StoreUiState.Empty
            assertEquals("없는 매장", empty.query)
            assertEquals("검색 결과가 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failureProducesErrorState() = runTest {
        val viewModel = StoreViewModel(
            FakeStoreRepository(stores = AppResult.Failure(DomainError.Network)),
        )

        viewModel.uiState.test {
            val state = awaitSettledState()

            assertTrue(state is StoreUiState.Error)
            val error = state as StoreUiState.Error
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storeClickLoadsBottomSheetDetail() = runTest {
        val viewModel = StoreViewModel(
            FakeStoreRepository(stores = AppResult.Success(listOf(sampleStore(id = "gangnam")))),
        )

        viewModel.uiState.test {
            assertTrue(awaitSettledState() is StoreUiState.Content)

            viewModel.onStoreClick("gangnam")

            val content = awaitItem() as StoreUiState.Content
            val detail = content.selectedStore
            assertEquals("카페민수 강남점", detail?.name)
            assertEquals("영업중 · 22:00 마감", detail?.statusLabel)
            assertEquals("현재 위치에서 120m", detail?.distanceLabel)
            assertEquals(listOf("콘센트", "Wi-Fi", "드라이브스루", "테라스"), detail?.amenities)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startOrderSelectsStoreAndEmitsMenuNavigation() = runTest {
        val repository = FakeStoreRepository(
            stores = AppResult.Success(listOf(sampleStore(id = "gangnam"))),
        )
        val viewModel = StoreViewModel(repository)

        viewModel.events.test {
            viewModel.onStartOrder("gangnam")

            assertEquals(StoreEvent.NavigateToMenu, awaitItem())
            assertEquals("gangnam", repository.selectedStore.value?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleStore(
        id: String,
        name: String = "카페민수 강남점",
        distanceMeters: Int = 120,
        status: StoreStatus = StoreStatus.Open,
    ): Store =
        Store(
            id = id,
            name = name,
            address = "서울 강남구 테헤란로 134",
            phone = "02-3456-7890",
            distanceMeters = distanceMeters,
            latitude = 37.498,
            longitude = 127.028,
            status = status,
            closingTimeLabel = "22:00 마감",
            amenities = listOf(
                StoreAmenity.Outlet,
                StoreAmenity.Wifi,
                StoreAmenity.DriveThru,
                StoreAmenity.Terrace,
                StoreAmenity.Parking,
            ),
        )

    private suspend fun ReceiveTurbine<StoreUiState>.awaitSettledState(): StoreUiState {
        val state = awaitItem()
        return if (state == StoreUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }
}

private class FakeStoreRepository(
    stores: AppResult<List<Store>>,
) : StoreRepository {
    private val storeState = MutableStateFlow(stores)
    val selectedStore = MutableStateFlow<Store?>(null)

    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        storeState.map { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    result.data.filter { store ->
                        query.isNullOrBlank() ||
                            store.name.contains(query) ||
                            store.address.contains(query)
                    },
                )

                is AppResult.Failure -> result
            }
        }

    override suspend fun getStore(storeId: String): AppResult<Store> =
        when (val result = storeState.value) {
            is AppResult.Success -> result.data
                .firstOrNull { it.id == storeId }
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(DomainError.NotFound)

            is AppResult.Failure -> result
        }

    override suspend fun selectStore(storeId: String): AppResult<Unit> =
        when (val result = getStore(storeId)) {
            is AppResult.Success -> {
                selectedStore.value = result.data
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    override fun observeSelectedStore(): Flow<Store?> = selectedStore
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
