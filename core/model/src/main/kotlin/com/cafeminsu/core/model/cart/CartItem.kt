package com.cafeminsu.core.model.cart

data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)
