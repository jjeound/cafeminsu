package com.cafeminsu.ui.feature.owner.menu

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import com.cafeminsu.domain.repository.OwnerMenuRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerMenuAddViewModelTest {
    @get:Rule
    val mainDispatcherRule: TestWatcher = OwnerMenuAddMainDispatcherRule()

    @Test
    fun submitStaysDisabledUntilNameAndPriceProvided() = runTest {
        val viewModel = OwnerMenuAddViewModel(FakeAddRepository())

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onNameChange("아메리카노")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPriceChange("4500")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun priceChangeKeepsDigitsOnly() = runTest {
        val viewModel = OwnerMenuAddViewModel(FakeAddRepository())

        viewModel.onPriceChange("4,500원")

        assertEquals("4500", viewModel.uiState.value.priceInput)
    }

    @Test
    fun submitSendsTrimmedDraftAndEmitsSaved() = runTest {
        val repository = FakeAddRepository()
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onCategorySelected(OwnerMenuAddCategory.Dessert)
        viewModel.onNameChange("  티라미수  ")
        viewModel.onPriceChange("6500")
        viewModel.onDescriptionChange("마스카포네 듬뿍")
        viewModel.onImagePicked("content://img/1")
        viewModel.onSaleToggle(false)

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(OwnerMenuAddEvent.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val draft = requireNotNull(repository.lastDraft)
        assertEquals("티라미수", draft.name)
        assertEquals("dessert", draft.categoryId)
        assertEquals(6500, draft.basePrice)
        assertEquals("마스카포네 듬뿍", draft.description)
        assertEquals("content://img/1", draft.imageUrl)
        assertTrue(draft.isSoldOut)
        assertEquals(1, repository.addMenuCallCount)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun submitWithoutPickedImageSendsNullImageUrl() = runTest {
        val repository = FakeAddRepository()
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onNameChange("아메리카노")
        viewModel.onPriceChange("4500")

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(OwnerMenuAddEvent.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // 이미지를 고르지 않으면 imageUrl 은 null 로 전달돼야 한다(잘못된 빈 값/경로 금지).
        assertEquals(null, requireNotNull(repository.lastDraft).imageUrl)
    }

    @Test
    fun submitWithPickedImageForwardsLocalUriToRepository() = runTest {
        val repository = FakeAddRepository()
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onNameChange("아메리카노")
        viewModel.onPriceChange("4500")
        viewModel.onImagePicked("content://media/external/images/7")

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(OwnerMenuAddEvent.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // 고른 로컬 URI 는 그대로 draft 로 전달되고, 업로드는 데이터 레이어에서 처리한다.
        assertEquals("content://media/external/images/7", requireNotNull(repository.lastDraft).imageUrl)
    }

    @Test
    fun submitFailureEmitsSnackbarAndClearsSubmitting() = runTest {
        val repository = FakeAddRepository(result = AppResult.Failure(DomainError.Network))
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onNameChange("아메리카노")
        viewModel.onPriceChange("4500")

        viewModel.events.test {
            viewModel.onSubmit()
            assertTrue(awaitItem() is OwnerMenuAddEvent.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun addsEditsOptionGroupsAndSubmitMapsToDraft() = runTest {
        val repository = FakeAddRepository()
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onNameChange("아메리카노")
        viewModel.onPriceChange("4500")

        viewModel.onAddOptionGroup()
        val groupId = viewModel.uiState.value.optionGroups.first().id
        viewModel.onOptionGroupNameChange(groupId, "사이즈")

        // a fresh group already contains one option row
        val firstOptionId = viewModel.uiState.value.optionGroups.first().options.first().id
        viewModel.onOptionNameChange(groupId, firstOptionId, "Tall")

        viewModel.onAddOption(groupId)
        val secondOptionId = viewModel.uiState.value.optionGroups.first().options[1].id
        viewModel.onOptionNameChange(groupId, secondOptionId, "Grande")
        viewModel.onOptionPriceChange(groupId, secondOptionId, "500")

        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(OwnerMenuAddEvent.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val draft = requireNotNull(repository.lastDraft)
        assertEquals(1, draft.options.size)
        val group = draft.options.first()
        assertEquals(groupId, group.id)
        assertEquals("사이즈", group.name)
        assertFalse(group.required)
        assertEquals(0, group.minSelect)
        assertEquals(2, group.maxSelect)
        assertEquals(listOf("Tall", "Grande"), group.options.map { it.name })
        assertEquals(0, group.options[0].extraPrice)
        assertEquals(500, group.options[1].extraPrice)
        assertTrue(group.options.all { it.isAvailable })
    }

    @Test
    fun removeOptionAndGroupUpdatesState() = runTest {
        val viewModel = OwnerMenuAddViewModel(FakeAddRepository())

        viewModel.onAddOptionGroup()
        val groupId = viewModel.uiState.value.optionGroups.first().id
        viewModel.onAddOption(groupId)
        assertEquals(2, viewModel.uiState.value.optionGroups.first().options.size)

        val optionId = viewModel.uiState.value.optionGroups.first().options.last().id
        viewModel.onRemoveOption(groupId, optionId)
        assertEquals(1, viewModel.uiState.value.optionGroups.first().options.size)

        viewModel.onRemoveOptionGroup(groupId)
        assertTrue(viewModel.uiState.value.optionGroups.isEmpty())
    }

    @Test
    fun optionPriceChangeKeepsDigitsOnly() = runTest {
        val viewModel = OwnerMenuAddViewModel(FakeAddRepository())
        viewModel.onAddOptionGroup()
        val groupId = viewModel.uiState.value.optionGroups.first().id
        val optionId = viewModel.uiState.value.optionGroups.first().options.first().id

        viewModel.onOptionPriceChange(groupId, optionId, "1,000원")

        assertEquals("1000", viewModel.uiState.value.optionGroups.first().options.first().priceInput)
    }

    @Test
    fun duplicateSubmitTapsCallRepositoryOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAddRepository(gate = gate)
        val viewModel = OwnerMenuAddViewModel(repository)
        viewModel.onNameChange("아메리카노")
        viewModel.onPriceChange("4500")

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.onSubmit()
        gate.complete(Unit)

        assertEquals(1, repository.addMenuCallCount)
    }
}

private class FakeAddRepository(
    private val result: AppResult<MenuItem> = AppResult.Success(addedMenu()),
    private val gate: CompletableDeferred<Unit>? = null,
) : OwnerMenuRepository {
    var lastDraft: NewMenuDraft? = null
        private set
    var addMenuCallCount = 0
        private set

    override fun observeManagedMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        flowOf(AppResult.Success(emptyList()))

    override suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem> =
        AppResult.Failure(DomainError.Unknown)

    override suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem> =
        AppResult.Failure(DomainError.Unknown)

    override suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem> {
        addMenuCallCount++
        lastDraft = draft
        gate?.await()
        return result
    }
}

private fun addedMenu(): MenuItem =
    MenuItem(
        id = "new-menu",
        categoryId = "coffee",
        name = "아메리카노",
        description = "",
        basePrice = 4_500,
        imageUrl = null,
        isSoldOut = false,
        options = emptyList(),
    )

@OptIn(ExperimentalCoroutinesApi::class)
private class OwnerMenuAddMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
