package com.cafeminsu.ui.feature.gift.claim

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.GiftSendRequest
import com.cafeminsu.domain.model.GiftSendResult
import com.cafeminsu.domain.repository.GiftRepository
import com.cafeminsu.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class GiftClaimViewModelTest {
    @get:Rule
    val mainDispatcherRule = GiftClaimMainDispatcherRule()

    @Test
    fun prefillsCodeFromValidDeepLinkArgument() = runTest {
        val viewModel = viewModel(initialCode = "GFT-1234-5678")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("GFT-1234-5678", state.code)
            assertTrue(state.canSubmit)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ignoresMalformedDeepLinkArgument() = runTest {
        val viewModel = viewModel(initialCode = "한글코드")

        viewModel.uiState.test {
            assertEquals("", awaitItem().code)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isDeepLinkClaimTrueOnlyForValidPrefilledCode() = runTest {
        // 딥링크로 유효한 코드가 채워진 경우만 자동 등록 대상.
        assertTrue(viewModel(initialCode = "GFT-1234-5678").isDeepLinkClaim)
        assertFalse(viewModel(initialCode = "").isDeepLinkClaim)
        assertFalse(viewModel(initialCode = "ab").isDeepLinkClaim)
    }

    @Test
    fun successfulClaimEmitsClaimedEventAndClearsSubmitting() = runTest {
        val repository = FakeGiftRepository(AppResult.Success(gifticon()))
        val viewModel = viewModel(giftRepository = repository, initialCode = "GFT-1234-5678")

        viewModel.events.test {
            viewModel.claim()

            val event = awaitItem()
            assertTrue(event is GiftClaimEvent.Claimed)
            assertEquals("GFT-1234-5678", repository.claims.single())
            assertFalse(viewModel.uiState.value.submitting)
            assertNull(viewModel.uiState.value.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failedClaimShowsErrorMessageWithoutEvent() = runTest {
        val repository = FakeGiftRepository(AppResult.Failure(DomainError.NotFound))
        val viewModel = viewModel(giftRepository = repository, initialCode = "GFT-1234-5678")

        viewModel.uiState.test {
            assertNull(awaitItem().errorMessage)

            viewModel.claim()

            val errored = awaitNonSubmitting()
            assertFalse(errored.submitting)
            assertEquals("등록할 수 없는 코드예요. 코드를 다시 확인해 주세요", errored.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invalidCodeDoesNotCallRepository() = runTest {
        val repository = FakeGiftRepository(AppResult.Success(gifticon()))
        val viewModel = viewModel(giftRepository = repository, initialCode = "")

        viewModel.onCodeChanged("ab")
        viewModel.claim()

        assertTrue(repository.claims.isEmpty())
        assertEquals("등록 코드를 확인해 주세요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun codeInputDropsDisallowedCharacters() = runTest {
        val viewModel = viewModel(initialCode = "")

        viewModel.onCodeChanged("GFT 1234!@#")

        assertEquals("GFT-1234".replace("-", ""), viewModel.uiState.value.code.replace("-", ""))
    }

    private fun viewModel(
        giftRepository: FakeGiftRepository = FakeGiftRepository(AppResult.Success(gifticon())),
        initialCode: String = "",
    ): GiftClaimViewModel =
        GiftClaimViewModel(
            giftRepository = giftRepository,
            savedStateHandle = SavedStateHandle(mapOf(Routes.GIFT_CLAIM_CODE to initialCode)),
        )

    private suspend fun app.cash.turbine.ReceiveTurbine<GiftClaimUiState>.awaitNonSubmitting(): GiftClaimUiState {
        var state = awaitItem()
        while (state.submitting) {
            state = awaitItem()
        }
        return state
    }

    private fun gifticon(): Gifticon =
        Gifticon(
            id = "gifticon-1",
            title = "금액형 기프티콘",
            barcodeValue = "barcode",
            qrValue = "qr",
            expiresAtMillis = 1_803_974_400_000L,
            status = GifticonStatus.Available,
        )
}

private class FakeGiftRepository(
    private val result: AppResult<Gifticon>,
) : GiftRepository {
    val claims = mutableListOf<String>()

    override suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult> =
        AppResult.Failure(DomainError.Unknown)

    override suspend fun claimGift(claimCode: String): AppResult<Gifticon> {
        claims += claimCode
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GiftClaimMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
