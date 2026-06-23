package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [CartRepository.clear] 호출 횟수만 기록하는 테스트용 페이크.
 * 매장 전환 시 장바구니 초기화 계약 검증에 사용한다.
 */
internal class RecordingCartRepository : CartRepository {
    var clearCount: Int = 0
        private set

    private val emptyCart = Cart(
        items = emptyList(),
        subtotal = 0,
        validation = CartValidation.Invalid(listOf(CartInvalidReason.Empty)),
    )
    private val cartState = MutableStateFlow<AppResult<Cart>>(AppResult.Success(emptyCart))

    override fun observeCart(): Flow<AppResult<Cart>> = cartState

    override suspend fun addItem(
        menuItemId: String,
        options: List<SelectedOption>,
        quantity: Int,
    ): AppResult<Cart> = AppResult.Success(emptyCart)

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart> =
        AppResult.Success(emptyCart)

    override suspend fun removeItem(cartItemId: String): AppResult<Cart> =
        AppResult.Success(emptyCart)

    override suspend fun validateForCheckout(): AppResult<CartValidation> =
        AppResult.Success(emptyCart.validation)

    override suspend fun clear(): AppResult<Unit> {
        clearCount++
        return AppResult.Success(Unit)
    }
}
