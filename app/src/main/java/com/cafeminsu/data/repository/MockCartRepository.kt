package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// 서버 카트가 없어 카트는 로컬 보관이지만, 메뉴 정보(가격·품절·옵션)는 활성 MenuRepository(Mock/Real)에서
// 해석한다. 그래야 서버 메뉴 id로도 담기·검증이 동작한다(MockData 하드코딩 목록에 의존하지 않음).
@Singleton
class MockCartRepository @Inject constructor(
    private val menuRepository: MenuRepository,
) : CartRepository {
    private val cartState = MutableStateFlow(AppResult.Success(emptyCart()))
    private val resolvedMenus = mutableMapOf<String, MenuItem>()
    private var nextCartItemNumber = 1

    override fun observeCart(): Flow<AppResult<Cart>> = cartState

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> {
        if (quantity <= 0) {
            return AppResult.Failure(DomainError.Validation("quantity"))
        }

        val menu = when (val result = menuRepository.getMenu(menuItemId)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        resolvedMenus[menu.id] = menu
        val selectedOptions = canonicalOptions(menu, options)
            ?: return AppResult.Failure(DomainError.Validation("options"))

        val unitPrice = menu.basePrice + selectedOptions.sumOf { it.extraPrice }
        val item = CartItem(
            id = "cart-item-${nextCartItemNumber++}",
            menuItemId = menu.id,
            name = menu.name,
            unitPrice = unitPrice,
            selectedOptions = selectedOptions,
            quantity = quantity,
        )
        return updateItems(currentItems() + item)
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> {
        val items = currentItems()
        if (items.none { it.id == cartItemId }) {
            return AppResult.Failure(DomainError.NotFound)
        }

        val updatedItems = if (quantity <= 0) {
            items.filterNot { it.id == cartItemId }
        } else {
            items.map { item ->
                if (item.id == cartItemId) {
                    item.copy(quantity = quantity)
                } else {
                    item
                }
            }
        }
        return updateItems(updatedItems)
    }

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> {
        val items = currentItems()
        if (items.none { it.id == cartItemId }) {
            return AppResult.Failure(DomainError.NotFound)
        }
        return updateItems(items.filterNot { it.id == cartItemId })
    }

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        AppResult.Success(cartState.value.dataOrEmptyCart().validation)

    override suspend fun clear(): AppResult<Unit> {
        cartState.value = AppResult.Success(emptyCart())
        return AppResult.Success(Unit)
    }

    private fun updateItems(items: List<CartItem>): AppResult<Cart> {
        val cart = buildCart(items)
        cartState.value = AppResult.Success(cart)
        return AppResult.Success(cart)
    }

    private fun buildCart(items: List<CartItem>): Cart {
        val subtotal = items.sumOf { item -> item.unitPrice * item.quantity }
        return Cart(
            items = items,
            subtotal = subtotal,
            validation = validateItems(items),
        )
    }

    private fun validateItems(items: List<CartItem>): CartValidation {
        if (items.isEmpty()) {
            return CartValidation.Invalid(listOf(CartInvalidReason.Empty))
        }

        val reasons = buildList {
            items.forEach { item ->
                val menu = resolvedMenus[item.menuItemId]
                if (menu?.isSoldOut == true) {
                    add(CartInvalidReason.SoldOut(item.menuItemId))
                }
            }
        }

        return if (reasons.isEmpty()) {
            CartValidation.Valid
        } else {
            CartValidation.Invalid(reasons)
        }
    }

    private fun canonicalOptions(
        menu: MenuItem,
        selectedOptions: List<SelectedOption>,
    ): List<SelectedOption>? =
        selectedOptions.map { selected ->
            val optionGroup = menu.options.firstOrNull { it.id == selected.groupId } ?: return null
            val option = optionGroup.options.firstOrNull { it.id == selected.optionId } ?: return null
            if (!option.isAvailable) {
                return null
            }
            SelectedOption(
                groupId = optionGroup.id,
                optionId = option.id,
                name = option.name,
                extraPrice = option.extraPrice,
            )
        }

    private fun currentItems(): List<CartItem> =
        cartState.value.dataOrEmptyCart().items

    private fun emptyCart(): Cart =
        buildCart(emptyList())

    private fun AppResult<Cart>.dataOrEmptyCart(): Cart =
        when (this) {
            is AppResult.Success -> data
            is AppResult.Failure -> emptyCart()
        }
}
