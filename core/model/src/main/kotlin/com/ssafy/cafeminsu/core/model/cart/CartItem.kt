package com.ssafy.cafeminsu.core.model.cart

data class CartItem(
    val id: String,
    val menuId: Long,
    val name: String,
    val image: String,
    val price: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)
