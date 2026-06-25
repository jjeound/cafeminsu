package com.cafeminsu.ui.feature.nfc

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.NfcCoupon
import com.cafeminsu.domain.repository.NfcCouponRepository
import kotlinx.coroutines.CompletableDeferred
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
class NfcClaimViewModelTest {
    @get:Rule
    val mainDispatcherRule = NfcClaimMainDispatcherRule()

    @Test
    fun successfulTagReadEmitsClaimedEventAndFillsResult() = runTest {
        val repository = FakeNfcCouponRepository(AppResult.Success(coupon()))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.events.test {
            viewModel.onTagRead("NFC-AB7K-9QM2")

            val event = awaitItem()
            assertTrue(event is NfcClaimEvent.Claimed)
            assertEquals(listOf("NFC-AB7K-9QM2"), repository.claims)

            val state = viewModel.uiState.value
            assertFalse(state.claiming)
            assertNull(state.errorMessage)
            assertEquals("1,000원", state.claimedCoupon?.amountLabel)
            assertEquals("2026.12.25 까지", state.claimedCoupon?.expiresLabel)
            assertEquals("오늘 하루 수고했어요", state.claimedCoupon?.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun claimingTogglesTrueThenFalse() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeNfcCouponRepository(AppResult.Success(coupon()), gate = gate)
        val viewModel = NfcClaimViewModel(repository)

        viewModel.uiState.test {
            assertFalse(awaitItem().claiming)

            viewModel.onTagRead("NFC-AB7K-9QM2")
            assertTrue(awaitItem().claiming)

            gate.complete(Unit)
            assertFalse(awaitItem().claiming)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun malformedPayloadShowsErrorWithoutCallingRepository() = runTest {
        val repository = FakeNfcCouponRepository(AppResult.Success(coupon()))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("한글태그")

        assertTrue(repository.claims.isEmpty())
        assertEquals("유효하지 않은 태그예요", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.claiming)
    }

    @Test
    fun reTagWhileClaimingIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeNfcCouponRepository(AppResult.Success(coupon()), gate = gate)
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")
        // 진행중(claiming)에 따닥 재태깅 → 무시되어야 함.
        viewModel.onTagRead("NFC-AB7K-9QM2")

        assertEquals(1, repository.claims.size)

        gate.complete(Unit)
    }

    @Test
    fun cooldownErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Payment("nfc-cooldown"),
            "오늘은 이미 받았어요. 내일 다시 시도해 주세요",
        )

    @Test
    fun inactiveErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Payment("nfc-inactive"),
            "사용할 수 없는 태그예요",
        )

    @Test
    fun otherPaymentErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Payment("nfc-something"),
            "쿠폰을 발급할 수 없어요",
        )

    @Test
    fun notFoundErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.NotFound,
            "유효하지 않은 태그예요",
        )

    @Test
    fun unauthorizedErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Unauthorized,
            "로그인이 필요해요. 다시 로그인해 주세요",
        )

    @Test
    fun validationErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Validation("tagCode"),
            "태그를 다시 인식해 주세요",
        )

    @Test
    fun networkErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Network,
            "네트워크 연결을 확인하고 다시 시도해 주세요",
        )

    @Test
    fun timeoutErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Timeout,
            "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요",
        )

    @Test
    fun unknownErrorMapsToKoreanMessage() =
        assertErrorMessage(
            DomainError.Unknown,
            "쿠폰 발급에 실패했어요. 잠시 후 다시 시도해 주세요",
        )

    @Test
    fun thrownErrorIsTreatedAsUnknown() = runTest {
        val repository = FakeNfcCouponRepository(throwable = IllegalStateException("boom"))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")

        assertEquals(
            "쿠폰 발급에 실패했어요. 잠시 후 다시 시도해 주세요",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.claiming)
        assertNull(viewModel.uiState.value.claimedCoupon)
    }

    @Test
    fun consumeErrorClearsMessage() = runTest {
        val repository = FakeNfcCouponRepository(AppResult.Failure(DomainError.NotFound))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")
        assertEquals("유효하지 않은 태그예요", viewModel.uiState.value.errorMessage)

        viewModel.consumeError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun resetClearsState() = runTest {
        val repository = FakeNfcCouponRepository(AppResult.Success(coupon()))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")
        assertEquals("1,000원", viewModel.uiState.value.claimedCoupon?.amountLabel)

        viewModel.reset()
        assertEquals(NfcClaimUiState(), viewModel.uiState.value)
    }

    @Test
    fun unparsableExpiryFallsBackToRawString() = runTest {
        val repository = FakeNfcCouponRepository(
            AppResult.Success(coupon(expiresAtIso = "곧 만료")),
        )
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")

        assertEquals("곧 만료", viewModel.uiState.value.claimedCoupon?.expiresLabel)
    }

    private fun assertErrorMessage(error: DomainError, expected: String) = runTest {
        val repository = FakeNfcCouponRepository(AppResult.Failure(error))
        val viewModel = NfcClaimViewModel(repository)

        viewModel.onTagRead("NFC-AB7K-9QM2")

        assertEquals(expected, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.claiming)
        assertNull(viewModel.uiState.value.claimedCoupon)
    }

    private fun coupon(
        expiresAtIso: String = "2026-12-25T00:00:00Z",
    ): NfcCoupon =
        NfcCoupon(
            gifticonId = 123L,
            amount = 1_000,
            expiresAtIso = expiresAtIso,
            message = "오늘 하루 수고했어요",
        )
}

private class FakeNfcCouponRepository(
    private val result: AppResult<NfcCoupon>? = null,
    private val throwable: Throwable? = null,
    private val gate: CompletableDeferred<Unit>? = null,
) : NfcCouponRepository {
    val claims = mutableListOf<String>()

    override suspend fun claim(tagCode: String): AppResult<NfcCoupon> {
        claims += tagCode
        gate?.await()
        throwable?.let { throw it }
        return result ?: AppResult.Failure(DomainError.Unknown)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NfcClaimMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
