package com.cafeminsu.ui.feature.voice

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import com.cafeminsu.domain.voice.ParsedOrder
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.domain.voice.VoiceOrderInterpreter
import com.cafeminsu.domain.voice.VoiceRecognitionError
import com.cafeminsu.domain.voice.VoiceRecognitionEvent
import com.cafeminsu.domain.voice.VoiceRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
class VoiceViewModelTest {
    @get:Rule
    val mainDispatcherRule = VoiceMainDispatcherRule()

    @Test
    fun partialEventUpdatesListeningStateWithPartialText() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = viewModel(recognizer = recognizer)

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())

            viewModel.onPermissionResult(granted = true)
            assertEquals(VoiceUiState.Listening(partialText = ""), awaitItem())

            recognizer.emit(VoiceRecognitionEvent.Partial("아메리카노"))

            assertEquals(VoiceUiState.Listening(partialText = "아메리카노"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun finalEventParsesMatchingMenuIntoParsedState() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = viewModel(recognizer = recognizer)

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())
            viewModel.onPermissionResult(granted = true)
            assertEquals(VoiceUiState.Listening(partialText = ""), awaitItem())

            recognizer.emit(VoiceRecognitionEvent.Final("아메리카노 두 잔"))

            val parsed = awaitParsed()
            assertEquals("아메리카노 두 잔", parsed.transcript)
            assertEquals("americano", parsed.items.single().menuItemId)
            assertEquals(2, parsed.items.single().quantity)
            assertEquals(8_000, parsed.estimatedTotalAmount)
            assertEquals(97, parsed.confidencePercent)
            assertTrue(parsed.unmatched.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmAddsParsedItemsToCartAndEmitsNavigationEvent() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val cartRepository = FakeCartRepository()
        val viewModel = viewModel(
            recognizer = recognizer,
            cartRepository = cartRepository,
        )

        viewModel.uiState.test {
            val states = this
            viewModel.events.test {
                assertEquals(VoiceUiState.Idle, states.awaitItem())
                viewModel.onPermissionResult(granted = true)
                assertEquals(VoiceUiState.Listening(partialText = ""), states.awaitItem())

                recognizer.emit(VoiceRecognitionEvent.Final("아메리카노 두 잔"))
                states.awaitParsed()

                viewModel.onConfirm()

                val added = states.awaitItem()
                assertTrue(added is VoiceUiState.AddedToCart)
                assertEquals("americano", cartRepository.addRequests.single().menuItemId)
                assertEquals(2, cartRepository.addRequests.single().quantity)
                assertEquals(VoiceEvent.NavigateToCart, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingPermissionMovesToPermissionRequiredWithoutStartingRecognizer() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = viewModel(recognizer = recognizer)

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())

            viewModel.onPermissionResult(granted = false)

            assertEquals(VoiceUiState.PermissionRequired, awaitItem())
            assertEquals(0, recognizer.startCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recognizerErrorMovesToErrorStateWithoutCrashing() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = viewModel(recognizer = recognizer)

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())
            viewModel.onPermissionResult(granted = true)
            assertEquals(VoiceUiState.Listening(partialText = ""), awaitItem())

            recognizer.emit(VoiceRecognitionEvent.Error(VoiceRecognitionError.Network))

            val error = awaitItem()
            assertTrue(error is VoiceUiState.Error)
            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", (error as VoiceUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun finalEmitsInterpretingStateWhileLlmRuns() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(
            recognizer = recognizer,
            interpreter = FakeVoiceOrderInterpreter(gate = gate),
        )

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())
            viewModel.onPermissionResult(granted = true)
            assertEquals(VoiceUiState.Listening(partialText = ""), awaitItem())

            recognizer.emit(VoiceRecognitionEvent.Final("아메리카노 두 잔"))
            assertEquals(VoiceUiState.Interpreting("아메리카노 두 잔"), awaitItem())

            gate.complete(Unit)
            val parsed = awaitParsed()
            assertEquals(2, parsed.items.single().quantity)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun interpreterFailureMovesToErrorState() = runTest {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = viewModel(
            recognizer = recognizer,
            interpreter = FakeVoiceOrderInterpreter(result = AppResult.Failure(DomainError.Unknown)),
        )

        viewModel.uiState.test {
            assertEquals(VoiceUiState.Idle, awaitItem())
            viewModel.onPermissionResult(granted = true)
            assertEquals(VoiceUiState.Listening(partialText = ""), awaitItem())

            recognizer.emit(VoiceRecognitionEvent.Final("아메리카노 두 잔"))

            var state = awaitItem()
            while (state is VoiceUiState.Listening || state is VoiceUiState.Interpreting) {
                state = awaitItem()
            }
            assertTrue(state is VoiceUiState.Error)
            assertEquals("음성 주문을 처리하지 못했어요", (state as VoiceUiState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        recognizer: FakeVoiceRecognizer = FakeVoiceRecognizer(),
        interpreter: VoiceOrderInterpreter = FakeVoiceOrderInterpreter(),
        menuRepository: MenuRepository = FakeMenuRepository(AppResult.Success(seedMenu)),
        cartRepository: FakeCartRepository = FakeCartRepository(),
    ): VoiceViewModel =
        VoiceViewModel(
            voiceRecognizer = recognizer,
            voiceOrderInterpreter = interpreter,
            menuRepository = menuRepository,
            cartRepository = cartRepository,
        )

    private suspend fun ReceiveTurbine<VoiceUiState>.awaitParsed(): VoiceUiState.Parsed {
        while (true) {
            val state = awaitItem()
            if (state is VoiceUiState.Parsed) {
                return state
            }
            assertTrue(
                state is VoiceUiState.Listening || state is VoiceUiState.Interpreting,
            )
        }
    }

    private companion object {
        private val temperatureGroup = MenuOptionGroup(
            id = "temperature",
            name = "온도",
            required = true,
            minSelect = 1,
            maxSelect = 1,
            options = listOf(
                MenuOption(
                    id = "hot",
                    name = "Hot",
                    extraPrice = 0,
                    isAvailable = true,
                ),
                MenuOption(
                    id = "iced",
                    name = "Iced",
                    extraPrice = 0,
                    isAvailable = true,
                ),
            ),
        )

        private val seedMenu = listOf(
            MenuItem(
                id = "americano",
                categoryId = "coffee",
                name = "아메리카노",
                description = "고소한 기본 커피",
                basePrice = 4_000,
                imageUrl = null,
                isSoldOut = false,
                options = listOf(temperatureGroup),
            ),
        )
    }
}

private class FakeVoiceOrderInterpreter(
    private val result: AppResult<ParsedOrder> = AppResult.Success(americanoTwoOrder()),
    private val gate: CompletableDeferred<Unit>? = null,
) : VoiceOrderInterpreter {
    override suspend fun interpret(transcript: String, menu: List<MenuItem>): AppResult<ParsedOrder> {
        gate?.await()
        return result
    }
}

private fun americanoTwoOrder(): ParsedOrder =
    ParsedOrder(
        items = listOf(
            ParsedOrderItem(
                menuItemId = "americano",
                name = "아메리카노",
                quantity = 2,
                selectedOptions = emptyList(),
            ),
        ),
        unmatched = emptyList(),
    )

private class FakeVoiceRecognizer : VoiceRecognizer {
    private val mutableEvents = MutableSharedFlow<VoiceRecognitionEvent>(
        extraBufferCapacity = EventBufferCapacity,
    )
    var startCount = 0
        private set

    override val events: Flow<VoiceRecognitionEvent> = mutableEvents

    override fun start() {
        startCount += 1
    }

    override fun stop() = Unit

    override fun destroy() = Unit

    suspend fun emit(event: VoiceRecognitionEvent) {
        mutableEvents.emit(event)
    }

    private companion object {
        const val EventBufferCapacity = 16
    }
}

private class FakeMenuRepository(
    private val menusResult: AppResult<List<MenuItem>>,
) : MenuRepository {
    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        MutableStateFlow(AppResult.Success(emptyList()))

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        MutableStateFlow(menusResult)

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
        AppResult.Failure(DomainError.NotFound)

    override suspend fun refreshMenus(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeCartRepository(
    private val addResult: AppResult<Cart> = AppResult.Success(emptyCart()),
) : CartRepository {
    val addRequests = mutableListOf<AddRequest>()

    override fun observeCart(): Flow<AppResult<Cart>> = MutableStateFlow(AppResult.Success(emptyCart()))

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> {
        addRequests += AddRequest(
            menuItemId = menuItemId,
            options = options,
            quantity = quantity,
        )
        return addResult
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> =
        AppResult.Success(emptyCart())

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> =
        AppResult.Success(emptyCart())

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        AppResult.Success(CartValidation.Invalid(listOf(CartInvalidReason.Empty)))

    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)
}

private data class AddRequest(
    val menuItemId: String,
    val options: List<SelectedOption>,
    val quantity: Int,
)

private fun emptyCart(): Cart =
    Cart(
        items = emptyList(),
        subtotal = 0,
        minimumOrderAmount = 10_000,
        validation = CartValidation.Invalid(listOf(CartInvalidReason.Empty)),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
