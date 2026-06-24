package com.cafeminsu.core.data.repository.cart

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.data.model.toEntities
import com.cafeminsu.core.database.dao.CartDao
import com.cafeminsu.core.model.cart.Cart
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCartRepository @Inject constructor(
    private val cartDao: CartDao,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CartRepository {
    override fun getCartByStoreId(storeId: Long): Flow<Cart> =
        cartDao.getCartItemEntities(storeId)
            .mapLatest { cartItems ->
                cartItems.asExternalModel()
            }
            .flowOn(ioDispatcher)

    override suspend fun updateCart(
        storeId: Long,
        cart: Cart,
    ) {
        cartDao.clearAllCart()
        val entities = cart.toEntities(storeId)
        if (entities.isNotEmpty()) {
            cartDao.upsertCartItemEntities(entities)
        }
    }

    override suspend fun clear() {
        cartDao.clearAllCart()
    }
}
