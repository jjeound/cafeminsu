package com.cafeminsu.domain.voice

import com.cafeminsu.domain.model.SelectedOption

data class ParsedOrder(
    val items: List<ParsedOrderItem>,
    val unmatched: List<String>,
)

data class ParsedOrderItem(
    val menuItemId: String,
    val name: String,
    val quantity: Int,
    val selectedOptions: List<SelectedOption>,
    val isSoldOut: Boolean = false,
)
