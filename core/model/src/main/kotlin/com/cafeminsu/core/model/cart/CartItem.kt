package com.cafeminsu.core.model.cart

import com.cafeminsu.core.model.media.ImageSource

data class CartItem(
    val id: String,
    val menuId: Long,
    val name: String,
    val image: ImageSource,
    val price: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)
