package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.SelectedOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCartRepositoryTest {
    @Test
    fun addItemCalculatesSubtotalWithSelectedOptions() = runBlocking {
        val repository = MockCartRepository()
        val menu = availableMenuWithPricedOption()
        val pricedGroup = menu.options.first { group -> group.options.any { it.extraPrice > 0 } }
        val selectedOption = selectedOption(pricedGroup, pricedGroup.options.first { it.extraPrice > 0 })

        repository.observeCart().test {
            skipItems(1)

            repository.addItem(menu.id, listOf(selectedOption), quantity = 2)
            val cart = awaitCart()

            assertEquals((menu.basePrice + selectedOption.extraPrice) * 2, cart.subtotal)
            assertEquals(menu.basePrice + selectedOption.extraPrice, cart.items.single().unitPrice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateQuantityAndRemoveItemRecalculateCart() = runBlocking {
        val repository = MockCartRepository()
        val menu = availableMenu()
        val added = repository.addItem(menu.id, emptyList(), quantity = 1).successData()
        val cartItemId = added.items.single().id

        val updated = repository.updateQuantity(cartItemId, quantity = 3).successData()
        assertEquals(menu.basePrice * 3, updated.subtotal)

        val removed = repository.removeItem(cartItemId).successData()
        assertTrue(removed.items.isEmpty())
        assertEquals(0, removed.subtotal)
    }

    @Test
    fun updateQuantityAtZeroRemovesItem() = runBlocking {
        val repository = MockCartRepository()
        val added = repository.addItem(availableMenu().id, emptyList(), quantity = 1).successData()

        val cart = repository.updateQuantity(added.items.single().id, quantity = 0).successData()

        assertTrue(cart.items.isEmpty())
        assertEquals(CartValidation.Invalid(listOf(CartInvalidReason.Empty)), cart.validation)
    }

    @Test
    fun validateForCheckoutReturnsEmptyWhenCartHasNoItems() = runBlocking {
        val repository = MockCartRepository()

        val validation = repository.validateForCheckout().successData()

        assertEquals(CartValidation.Invalid(listOf(CartInvalidReason.Empty)), validation)
    }

    @Test
    fun validateForCheckoutReturnsBelowMinimumAmount() = runBlocking {
        val repository = MockCartRepository()
        val menu = availableMenu()

        val cart = repository.addItem(menu.id, emptyList(), quantity = 1).successData()

        assertEquals(
            CartValidation.Invalid(
                listOf(CartInvalidReason.BelowMinimumAmount(MockData.minimumOrderAmount - cart.subtotal)),
            ),
            cart.validation,
        )
    }

    @Test
    fun validateForCheckoutReturnsSoldOutReason() = runBlocking {
        val repository = MockCartRepository()
        val soldOutMenu = MockData.menuItems.first { it.isSoldOut }
        val quantity = quantityToMeetMinimum(soldOutMenu)

        val cart = repository.addItem(soldOutMenu.id, emptyList(), quantity).successData()

        assertTrue(cart.validation is CartValidation.Invalid)
        assertTrue(
            (cart.validation as CartValidation.Invalid).reasons.contains(
                CartInvalidReason.SoldOut(soldOutMenu.id),
            ),
        )
    }

    @Test
    fun validateForCheckoutReturnsValidWhenCartMeetsRules() = runBlocking {
        val repository = MockCartRepository()
        val menu = availableMenu()
        val quantity = quantityToMeetMinimum(menu)

        val cart = repository.addItem(menu.id, emptyList(), quantity).successData()

        assertEquals(CartValidation.Valid, cart.validation)
        assertEquals(CartValidation.Valid, repository.validateForCheckout().successData())
    }

    private fun availableMenu(): MenuItem =
        MockData.menuItems.first { !it.isSoldOut }

    private fun availableMenuWithPricedOption(): MenuItem =
        MockData.menuItems.first { menu ->
            !menu.isSoldOut && menu.options.any { group -> group.options.any { it.extraPrice > 0 } }
        }

    private fun selectedOption(group: MenuOptionGroup, option: MenuOption): SelectedOption =
        SelectedOption(
            groupId = group.id,
            optionId = option.id,
            name = option.name,
            extraPrice = option.extraPrice,
        )

    private fun quantityToMeetMinimum(menu: MenuItem): Int =
        (MockData.minimumOrderAmount / menu.basePrice) + 1

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<AppResult<Cart>>.awaitCart(): Cart {
        val result = awaitItem()
        return result.successData()
    }
}
